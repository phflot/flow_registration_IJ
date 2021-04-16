package floregistration.algorithm;

import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.real.FloatType;

public class RegistrationResult<T extends NativeType<T> & ComplexType<T>, A extends ArrayDataAccess<A>> {
	
	private final ImagePlusImg<T, A> registered;
	private final PlanarImg<FloatType, FloatArray> w;
	private final float meanDiv;

	private final float meanDisp;
	private final float maxDisp;
	
	public RegistrationResult(ImagePlusImg<T, A> registered, 
			PlanarImg<FloatType, FloatArray> w, 
			float meanDiv, float meanDisp, float maxDisp) {
		this.registered = registered;
		this.w = w;
		this.meanDiv = meanDiv;
		this.meanDisp = meanDisp;
		this.maxDisp = maxDisp;
	}
	
	public ImagePlusImg<T, A> getRegistered() {
		return registered;
	}

	public PlanarImg<FloatType, FloatArray> getW() {
		return w;
	}

	public float getMeanDiv() {
		return meanDiv;
	}

	public float getMeanDisp() {
		return meanDisp;
	}

	public float getMaxDisp() {
		return maxDisp;
	}
}