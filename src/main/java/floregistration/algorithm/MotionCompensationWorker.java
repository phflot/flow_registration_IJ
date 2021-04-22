package floregistration.algorithm;

import java.beans.PropertyChangeSupport;
import java.util.LinkedList;
import java.util.stream.IntStream;

import javax.swing.SwingWorker;

import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Collects the options and images and handles the image registration calls per frame
 */
public class MotionCompensationWorker extends SwingWorker<Void, Void> {

	private final OFsolver ofInstance;
	private final RegistrationJob registrationJob;
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private int nProcessedFrames = 0;
	private int framesLeft;
	private long startTime;
	
	
	public MotionCompensationWorker(RegistrationJob registrationJob) {
		ofInstance = registrationJob.getOFinstance();
		this.registrationJob = registrationJob;
		framesLeft = registrationJob.getNslices();
	}
	
	
	private synchronized void processedFrameNotification() {
		nProcessedFrames++;
		framesLeft--;
		firePropertyChange("n_processed_frames", nProcessedFrames-1, nProcessedFrames);
		firePropertyChange("frames_left", framesLeft+1, framesLeft);
		double tmp = (double)(System.currentTimeMillis() - startTime) / 1000;
		firePropertyChange("elapsed", tmp-1, tmp);
	}	

	
	private class IntermediateStruct<T extends NativeType<T> & ComplexType<T>, A extends ArrayDataAccess<A>> {
		
		private final RegistrationChannelOptions<T, A> o;
		private final ImagePlusImg<T, A> registrationTarget;
		private final ImagePlusImg<T, A> imgLow;
		
		private final float minValue;
		private final float maxValue;
		private final int width;
		private final int height;
		private final boolean isInplace;
		
		public boolean isInplace() {
			return isInplace;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public int getnSlices() {
			return nSlices;
		}
			
		public ImagePlusImg<T, A> getRegistrationTarget() {
			return registrationTarget;
		}

		private final int nSlices;
		
		@SuppressWarnings("unchecked")
		public IntermediateStruct(RegistrationChannelOptions<T, A> o) {
			this.o = o;
			
			long[] dimsL = new long[o.getImg().numDimensions()];
			o.getImg().dimensions(dimsL);
			int[] dims = new int[o.getImg().numDimensions()];
			int counter = 0;
			for (long l : dimsL) {
				dims[counter] = (int)Math.max(Math.min(l, Integer.MAX_VALUE), Integer.MIN_VALUE);
				counter++;
			}
			
			this.width = dims[0];
			this.height = dims[1];
			this.nSlices = dims[2];
			
			ImagePlusImg<T, A> imgLow = (ImagePlusImg<T, A>) 
					o.getImg().factory().create(width, height, nSlices);

			float[] s = o.getSigma();
			Gauss3.gauss(new double[] {s[0], s[1], s[2]}, Views.extendBorder(o.getImg()), imgLow);
			
			this.imgLow = imgLow;
			minValue = Util.getMin(imgLow);
			maxValue = Util.getMax(imgLow);
			
			this.isInplace = o.isInplace();
			if (o.isInplace()) {
				registrationTarget = o.getImg();
			} else {
				registrationTarget = (ImagePlusImg<T, A>) o.getImg().factory().create(dims);
			}
		}
		
		public ImagePlusImg<FloatType, FloatArray> getFrameLow(int n) {
			
			@SuppressWarnings("unchecked")
			ImagePlusImg<T, A> imgTmp = (ImagePlusImg<T, A>) imgLow.factory().create(width, height);
			imgTmp.setPlane(0, imgLow.getPlane(n));
			
			ImagePlusImg<FloatType, FloatArray> frameOut = Util.imgToFloatNormalize(imgTmp, minValue, maxValue);

			return frameOut;
		}
		
		public ImagePlusImg<T, A> getFrame(int n) {
			@SuppressWarnings("unchecked")
			ImagePlusImg<T, A> imgTmp = (ImagePlusImg<T, A>) imgLow.factory().create(width, height);
			imgTmp.setPlane(0, o.getImg().getPlane(n));
			
			return imgTmp;
		}
	}
	
	private class IntermediateStructs<T extends NativeType<T> & ComplexType<T>, A extends ArrayDataAccess<A>> 
	extends LinkedList<IntermediateStruct<T, A>> {
		
		public ImagePlusImg<FloatType, FloatArray> getFramesLow(int n) {
			
			@SuppressWarnings("unchecked")
			ImagePlusImg<FloatType, FloatArray> out = 
					(ImagePlusImg<FloatType, FloatArray>) this.getFirst().getFrameLow(0).factory().create(
							this.getFirst().getWidth(), this.getFirst().getHeight(), this.size());
			
			for (int i = 0; i < this.size(); i++) {
				out.setPlane(i, this.get(i).getFrameLow(n).getPlane(0));
			}
			
			return out;
		}
		
		public ImagePlusImg<T, A> getFrames(int n) {
			
			@SuppressWarnings("unchecked")
			ImagePlusImg<T, A> out = 
					(ImagePlusImg<T, A>) this.getFirst().getFrame(0).factory().create(
							this.getFirst().getWidth(), this.getFirst().getHeight(), this.size());
			
			for (int i = 0; i < this.size(); i++) {
				out.setPlane(i, this.get(i).getFrame(n).getPlane(0));
			}
			
			return out;
		}
		
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -7248646765861061239L;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected Void doInBackground() {
		
		int width = registrationJob.getWidth();
		int height= registrationJob.getHeight();
		
		IntermediateStructs<?, ?> intermediateStructs = new IntermediateStructs();
		
		ImagePlusImgFactory<FloatType> factory = new ImagePlusImgFactory(new FloatType());

		OFsolver ofInstance = registrationJob.getOFinstance();
		
		for (RegistrationChannelOptions<?, ?> o : registrationJob) {
			IntermediateStruct tmp = new IntermediateStruct(o);
			intermediateStructs.add(tmp);
		}
		
		ImagePlusImg<FloatType, FloatArray> ref = intermediateStructs.getFramesLow(2);
		ImagePlusImg<FloatType, FloatArray> dataWeightArray = registrationJob.getDataWeightArray();
		ImagePlusImg<FloatType, FloatArray> wInit = (ImagePlusImg<FloatType, FloatArray>) factory.create(width, height, 2);
		
		startTime = System.currentTimeMillis();
		IntStream.rangeClosed(0, registrationJob.getNRegistrationTargets() - 1).parallel().forEach(n -> {
		//IntStream.rangeClosed(0, registrationJob.getNslices() - 1).forEach(n -> {
			int idx = registrationJob.getIdxAt(n);
			ImagePlusImg<FloatType, FloatArray> img = intermediateStructs.getFramesLow(idx);
			ImagePlusImg registrationTargets = intermediateStructs.getFrames(idx);
			
			RegistrationResult registrationResult = ofInstance.compensate(img, ref, 
					dataWeightArray, wInit, registrationTargets);
			
			for (int i = 0; i < intermediateStructs.size(); i++) {
				IntermediateStruct s = (IntermediateStruct) intermediateStructs.get(i);
				
				if (registrationJob.get(i).isInplace())
					s.getRegistrationTarget().setPlane(n, registrationResult.getRegistered().getPlane(i));
				else
					s.getRegistrationTarget().setPlane(idx, registrationResult.getRegistered().getPlane(i));
			}
			
			processedFrameNotification();
		});
		
		long elapsed = System.currentTimeMillis() - startTime;
		firePropertyChange("final_time", 0, (double)elapsed / 1000);
		
		for (IntermediateStruct s : intermediateStructs) {
			if (!s.isInplace)
				ImageJFunctions.show(s.registrationTarget);
		}
		
		return null;
	}
}
