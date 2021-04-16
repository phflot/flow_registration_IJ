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
	private int height = 0;
	private int width = 0;
	
	/**
	 * Solver Options
	 */
	private int iterations = 50;
	private int updateLag = 5;
	private int levels = 50;
	private float eta  = 0.75f;
	private final float[] alpha = new float[] {0.1f, 0.1f};
	
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
		}
		else {
			if (tmp_slices != nSlices || tmp_height != height
					|| tmp_width != width)
				return false;
		}
		return super.add(e);
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
				iterations, updateLag, levels, alpha, aData, 1.0f, eta, 0.001f);
		
		return new OFsolver(options);
	}
	
	private static final long serialVersionUID = 1L;

}
