package floregistration.algorithm;

import java.util.LinkedList;

import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.real.FloatType;

/**
 * Class that defines and stores all the parameters and images for
 * Image Registration
 *
 */
@SuppressWarnings("rawtypes")
public class RegistrationJob extends LinkedList<RegistrationChannelOptions>{
	
	private int nSlices = 0;
	private int stride = 1;
	private int[] registrationIDX;
	private int height = 0;
	private int width = 0;
	private int lowerIDX = 0;
	private int upperIDX = 0;
	private ReferenceFrames referenceFrames;
	
	private class ReferenceFrames {
		
		public int getLower() {
			return lower;
		}

		public void setLower(int lower) {
			this.lower = Math.min(lower, this.nSlices - 1);
		}

		public int getUpper() {
			return upper;
		}

		public void setUpper(int upper) {
			this.upper = Math.min(upper, this.nSlices - 1);
		}

		private int lower;
		private int upper;
		
		private int nSlices;
				
		public ReferenceFrames(int nSlices) {
			this.nSlices = nSlices;
			lower = 0;
			upper = Math.min(20, nSlices - 1);
		}		
	}
	
	
	/**
	 * Solver Options
	 */
	private int iterations = 50;
	private int updateLag = 5;
	private int levels = 100;
	private float eta  = 0.8f;
	private int minLevel = 0;
	private final float[] alpha = new float[] {1.5f, 1.5f};
	
	public int getIterations() {
		return iterations;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public int getUpdateLag() {
		return updateLag;
	}

	public void setUpdateLag(int updateLag) {
		this.updateLag = updateLag;
	}

	public int getLevels() {
		return levels;
	}

	public void setLevels(int levels) {
		this.levels = levels;
	}

	public float getEta() {
		return eta;
	}

	public void setEta(float eta) {
		this.eta = eta;
	}

	public float[] getAlpha() {
		return alpha;
	}
	
	private RegistrationSolverOptions solverOptions;
	
	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	public int getNslices() {
		return nSlices;
	}
	
	public void setMinLevel(int minLevel) {
		this.minLevel = minLevel;
	}
	
	public int getNRegistrationTargets() {
		return this.registrationIDX.length;
	}
	
	public int getIdxAt(int i) {
		return this.registrationIDX[i];
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean add(RegistrationChannelOptions e) {
		
		ImagePlusImg<? extends NativeType<?>, ?> img = e.getImg();
		
		int tmp_slices = img.numSlices();
		int tmp_height = img.getHeight();
		int tmp_width = img.getWidth();
		
		if (this.size() == 0) {
			this.nSlices = tmp_slices;
			this.height = tmp_height;
			this.width = tmp_width;
			this.upperIDX = this.nSlices - 1;
			this.referenceFrames = new ReferenceFrames(nSlices);
			updateIndices();
		}
		else {
			if (tmp_slices != nSlices || tmp_height != height
					|| tmp_width != width)
				return false;
		}
		return super.add(e);
	}
	
	@Override
	public RegistrationChannelOptions get(int i) {
		return (RegistrationChannelOptions) super.get(i);
	}
	
	
	public void setStride(int stride) {
		this.stride = stride > (this.upperIDX - this.lowerIDX) ? (this.upperIDX - this.lowerIDX) : stride;
		if (this.stride <= 1)
			this.stride = 1;
		updateIndices();
	}
	
	public void setRange(double lower, double upper) {
		assert lower <= 1.0f & lower >= 0.0f;
		assert upper <= 1.0f & upper >= 0.0f;
		
		this.lowerIDX = (int) Math.round((float)(this.nSlices - 1) * lower);
		this.upperIDX = (int) Math.round((float)(this.nSlices - 1) * upper);
		
		if (this.lowerIDX == this.upperIDX) {
			this.lowerIDX -= this.lowerIDX == 0 ? 0 : 1;
			this.upperIDX += this.lowerIDX == 0 ? 1 : 0;
		}
		updateIndices();
	}
	
	private void updateIndices() {
		int nRegistrationFrames = (int)Math.round((double)(this.upperIDX - this.lowerIDX) / (double)this.stride);
		this.registrationIDX = new int[nRegistrationFrames];
		for (int i = 0; i < nRegistrationFrames; i++) {
			this.registrationIDX[i] = i * this.stride + this.lowerIDX;
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public ImagePlusImg<FloatType, FloatArray> getDataWeightArray() {
		if (this.size() == 0)
			return null;
		
		ImagePlusImg<FloatType, FloatArray> dataWeightArray = 
				(ImagePlusImg<FloatType, FloatArray>) new ImagePlusImgFactory<>(new FloatType()).create(getWidth(), getHeight(), this.size());
		
		for (int i = 0; i < this.size(); i++) {
			float[] weightPixels = new float[getWidth() * getHeight()];
			RegistrationChannelOptions tmp = this.get(i);
			float weight = tmp.getWeight();
			for (int j = 0; j < getWidth() * getHeight(); j++) {
				weightPixels[j] = weight;
			}
			dataWeightArray.setPlane(i, new FloatArray(weightPixels));
		}
		
		return dataWeightArray;
	}
	
	public void setReferenceIDX(int lower, int upper) {
		this.referenceFrames.setLower(lower);
		this.referenceFrames.setUpper(upper);
	}
	
	//TODO: needs to be replaced by a method that directly calculates the mean frames!
	public int getMeanLowIDX() {
		return this.referenceFrames.getLower();
	}
	
	public int getMeanUpIDX() {
		return this.referenceFrames.getUpper();
	}
	
	public RegistrationJob() {
		super();
		this.solverOptions = new RegistrationSolverOptions();
	}
	
	public RegistrationJob(RegistrationSolverOptions solverOptions) {
		super();
		this.solverOptions = solverOptions;
	}
	
	public void setSolverOptions(RegistrationSolverOptions solverOptions) {
		this.solverOptions = solverOptions;
	}
	
	public RegistrationSolverOptions getSolverOptions() {
		return solverOptions;
	}
	
	/**
	 * Generates a new OF Instance with all relevant parameters
	 * @return
	 */
	public OFsolver getOFinstance() {
		
		float[] aData = new float[this.size()];

		for (int i = 0; i < this.size(); i++) {
			aData[i] = this.get(i).getAdata();
		}
		
		RegistrationSolverOptions options = new RegistrationSolverOptions(
				iterations, updateLag, levels, alpha, aData, eta, 0.001f);
		options.minLevel = minLevel;
		
		return new OFsolver(options);
	}
	
	private static final long serialVersionUID = 1L;

}
