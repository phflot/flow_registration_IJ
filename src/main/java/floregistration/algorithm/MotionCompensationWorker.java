package floregistration.algorithm;

import java.awt.Color;
import java.beans.PropertyChangeSupport;
import java.util.LinkedList;
import java.util.stream.IntStream;

import javax.swing.SwingWorker;

import ij.gui.Plot;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.cell.CellImgFactory;
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
		framesLeft = registrationJob.getNRegistrationTargets();
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
		private final ImagePlusImg<FloatType, FloatArray> imgLow;
		
		//private final float minValue;
		//private final float maxValue;
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
		private IntermediateStruct(RegistrationChannelOptions<T, A> o, int nFrames) {
			this.o = o;
			
			long[] dimsL = new long[o.getImg().numDimensions()];
			o.getImg().dimensions(dimsL);
			
			int[] dims = new int[o.getImg().numDimensions()];
			int counter = 0;
			for (long l : dimsL) {
				dims[counter] = (int)Math.max(Math.min(l, Integer.MAX_VALUE), Integer.MIN_VALUE);
				counter++;
			}
			//int[] dims = new int[4];
			//dims[0] = o.getImg().getWidth();
			//dims[1] = o.getImg().getHeight();
			//dims[2] = 1;
			//dims[3] = o.getImg().getDepth();
			//dims[4] = o.getImg().getDepth();
			
			int nSlicesOrig = dims[2];
			dims[2] = o.isInplace() ? dims[2] : nFrames;
			
			this.width = dims[0];
			this.height = dims[1];
			this.nSlices = dims[2];
			
			ImagePlusImg<T, A> imgLow = (ImagePlusImg<T, A>) 
					o.getImg().factory().create(width, height, nSlicesOrig);

			float[] s = o.getSigma();
			Gauss3.gauss(new double[] {s[0], s[1], s[2]}, Views.extendBorder(o.getImg()), imgLow);
			
			this.imgLow = Util.imgToFloatNormalize(imgLow);
			//minValue = Util.getMin(imgLow);
			//maxValue = Util.getMax(imgLow);
			
			this.isInplace = o.isInplace();
			if (o.isInplace()) {
				registrationTarget = o.getImg();
			} else {
				o.getImg().dimensions(dimsL);
				registrationTarget = (ImagePlusImg<T, A>) o.getImg().factory().create(dims[0], dims[1], 1, 1, dims[2]);
			}
		}
		
		public ImagePlusImg<FloatType, FloatArray> getFrameLow(int n) {
			
			@SuppressWarnings("unchecked")
			ImagePlusImg<FloatType, FloatArray> imgTmp = (ImagePlusImg<FloatType, FloatArray>) imgLow.factory().create(width, height);
			imgTmp.setPlane(0, imgLow.getPlane(n));
			
			// ImagePlusImg<FloatType, FloatArray> frameOut = Util.imgToFloatNormalize(imgTmp, minValue, maxValue);

			// return frameOut;
			return imgTmp;
		}
		
		public ImagePlusImg<FloatType, FloatArray> getFramesLow() {
			return (ImagePlusImg<FloatType, FloatArray>) imgLow;
		}
		
		public ImagePlusImg<T, A> getFrame(int n) {
			@SuppressWarnings("unchecked")
			ImagePlusImg<T, A> imgTmp = (ImagePlusImg<T, A>) o.getImg().factory().create(width, height);
			imgTmp.setPlane(0, o.getImg().getPlane(n));
			
			return imgTmp;
		}
	}
	
	private class IntermediateStructs<T extends NativeType<T> & ComplexType<T>, A extends ArrayDataAccess<A>> 
	extends LinkedList<IntermediateStruct<T, A>> {
		
		public ImagePlusImg<FloatType, FloatArray> getRefLow() {
			@SuppressWarnings("unchecked")
			ImagePlusImg<FloatType, FloatArray> out = 
					(ImagePlusImg<FloatType, FloatArray>) this.getFirst().getFrameLow(0).factory().create(
							this.getFirst().getWidth(), this.getFirst().getHeight(), this.size());	
			for (int i = 0; i < this.size(); i++) {
				out.setPlane(i, Util.getMean3f(this.get(i).getFramesLow(), 
						registrationJob.getMeanLowIDX(), registrationJob.getMeanUpIDX()).getPlane(0));
			}
			return out;
		}
		
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
			IntermediateStruct tmp = new IntermediateStruct(o, registrationJob.getNRegistrationTargets());
			intermediateStructs.add(tmp);
		}
		
		ImagePlusImg<FloatType, FloatArray> ref = intermediateStructs.getRefLow();
		ImagePlusImg<FloatType, FloatArray> dataWeightArray = registrationJob.getDataWeightArray();
		ImagePlusImg<FloatType, FloatArray> wInit = (ImagePlusImg<FloatType, FloatArray>) factory.create(width, height, 2);
		ImageJFunctions.showFloat(ref, "Reference Frames");
		
		float[] meanDivergence = new float[registrationJob.getNRegistrationTargets()];
		float[] meanDisp = new float[registrationJob.getNRegistrationTargets()];
		float[] maxDisp = new float[registrationJob.getNRegistrationTargets()];
		
		startTime = System.currentTimeMillis();
		IntStream.rangeClosed(0, registrationJob.getNRegistrationTargets() - 1).parallel().forEach(n -> {
		//IntStream.rangeClosed(0, registrationJob.getNRegistrationTargets() - 1).forEach(n -> {
			int idx = registrationJob.getIdxAt(n);
			ImagePlusImg<FloatType, FloatArray> img = intermediateStructs.getFramesLow(idx);
			ImagePlusImg registrationTargets = intermediateStructs.getFrames(idx);
			
			RegistrationResult registrationResult = ofInstance.compensate(img, ref, 
					dataWeightArray, wInit, registrationTargets);
			
			for (int i = 0; i < intermediateStructs.size(); i++) {
				IntermediateStruct s = (IntermediateStruct) intermediateStructs.get(i);
				
				if (s.isInplace())
					s.getRegistrationTarget().setPlane(idx, registrationResult.getRegistered().getPlane(i));
				else
					s.getRegistrationTarget().setPlane(n, registrationResult.getRegistered().getPlane(i));
			}
			
			meanDivergence[n] = registrationResult.getMeanDiv();
			meanDisp[n] = registrationResult.getMeanDisp();
			maxDisp[n] = registrationResult.getMaxDisp();
			
			processedFrameNotification();
		});
		
		
		final ImgFactory< FloatType > imgFactory = new CellImgFactory<>( new FloatType(), 5 );
		 
		// create an 3d-Img with dimensions 20x30x40 (here cellsize is 5x5x5)Ø
		final Img< FloatType > img1 = imgFactory.create( 20, 30, 1, 50 );
		ImageJFunctions.show( img1 );

		
		long elapsed = System.currentTimeMillis() - startTime;
		firePropertyChange("final_time", 0, (double)elapsed / 1000);
				
		float[] floatIDX = new float[registrationJob.getNRegistrationTargets()];
		for (int i = 0; i < floatIDX.length; i++) {
			floatIDX[i] = registrationJob.getIdxAt(i);
		}
		
		Plot divergencePlot = new Plot("Mean Divergence", "Samples", "Divergence");
		divergencePlot.setColor(Color.blue);
		divergencePlot.addPoints(floatIDX, meanDivergence, Plot.LINE);
		divergencePlot.addLegend("Mean Divergence");
		divergencePlot.show();
		
		Plot dispPlot = new Plot("Mean and Max Displacements", "Samples", "Displacements");
		dispPlot.setColor(Color.blue);
		dispPlot.addPoints(floatIDX, meanDisp, Plot.LINE);
		dispPlot.setColor(Color.red);
		dispPlot.addPoints(floatIDX, maxDisp, Plot.LINE);
		dispPlot.addLegend("Mean Displacements\nMax Displacements");
		dispPlot.setLimits(floatIDX[0], floatIDX[floatIDX.length-1], Util.getMinF(meanDisp), Util.getMaxF(maxDisp));
		dispPlot.show();
		
		int counter = 1;
		for (IntermediateStruct s : intermediateStructs) {
			if (!s.isInplace)
				ImageJFunctions.show(s.registrationTarget, "registered channel " + counter++);
		}
		
		return null;
	}
}
