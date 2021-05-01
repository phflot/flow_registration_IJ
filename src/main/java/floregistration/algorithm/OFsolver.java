package floregistration.algorithm;


import java.util.ArrayList;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;


public class OFsolver {
	
	
	private final float OMEGA = 1.95f;
	private int iterations;
	private int updateLag;
	private int levels;
	private float[] alpha;
	private float[] aData;
	private float aSmooth;
	private float eta;
	private float sigma;
	private final float eps = 0.00001f;
		
	
	public static void main( String[] args ) {
		demo();
	}

	
	public OFsolver() {
		this(new RegistrationSolverOptions());
	}
	
	
	public OFsolver(RegistrationSolverOptions options) {
		
		iterations = options.iterations;
		updateLag = options.updateLag;
		levels = options.levels;
		alpha = options.alpha;
		aData = options.aData;
		aSmooth = options.aSmooth;
		eta = options.eta;
		sigma = options.sigma;
	}

	
	public RegistrationResult<FloatType, FloatArray>
		compensate(
			PlanarImg<FloatType, FloatArray> img, 
			PlanarImg<FloatType, FloatArray> ref, 
			PlanarImg<FloatType, FloatArray> dataWeightVector, 
			PlanarImg<FloatType, FloatArray> wInit) {
		
		return compensate(img, ref, dataWeightVector, wInit, img);		
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T extends NativeType<T> & ComplexType<T>, A extends ArrayDataAccess<A>> RegistrationResult<T, A>
		compensate(
			PlanarImg<FloatType, FloatArray> img, 
			PlanarImg<FloatType, FloatArray> ref, 
			PlanarImg<FloatType, FloatArray> dataWeightVector, 
			PlanarImg<FloatType, FloatArray> wInit, 
			PlanarImg<T, A> registrationTargets) {
				
		PlanarImg<FloatType, FloatArray> w = doOF(img, ref, dataWeightVector, wInit);
				
		float meanDisp = Util.getMeanMagnitude(w);
		float maxDisp = Util.getMaxMagnitude(w);
		float meanDiv = Util.getMeanDivergence(w);
		
		ImagePlusImg registered = (ImagePlusImg) registrationTargets.copy();
		
		Util.warp(registrationTargets, w, registered);
				
		RegistrationResult result = new RegistrationResult(registered, w, meanDiv, meanDisp, maxDisp);
		
		return result;
	}
	
	
	@SuppressWarnings({ "unchecked" })
	public <I extends PlanarImg<FloatType, FloatArray>> I doOF(I img, 
			I ref, I dataWeightVector, I wInit) {
		
		int width = (int)img.dimension(0);
		int height = (int)img.dimension(1);
		int nChannels = img.numSlices();
		
		// a_data should be float[n_channels]:
		if (aData.length < nChannels) {
			float[] a_data_old = aData;
			aData = new float[nChannels];
			for (int i = 0; i < nChannels; i++)
				aData[i] = a_data_old.length < i ? a_data_old[i] : a_data_old[0];
		}
		// same for alpha
		if (alpha.length < 2) {
			float[] alpha_old = alpha;
			alpha = new float[2];
			for (int i = 0; i < 2; i++)
				alpha[i] = alpha_old.length < i ? alpha_old[i] : alpha_old[0];
		}
		
		int idx, idxLeft, idxRight, idxDown, idxUp, ndIdx;
		float wRight, wLeft, wDown, wUp, wSum, denomU, denomV, numU, numV;
		
		// for now only taking the first value of weight and normalizing it:
		float[] weight = new float[nChannels];
		float sum = 0;
		for (int i = 0; i < nChannels; i++) {
			float[] tmp = dataWeightVector.getPlane(i).getCurrentStorageArray();
			weight[i] = tmp[0];
			sum += tmp[0];
		}
		for (int i = 0; i < nChannels; i++) {
			float[] tmp = dataWeightVector.getPlane(i).getCurrentStorageArray();
			weight[i] /= sum;
		}
		
		int max_level = warpingDepth(height, width);
				
		// low pass filtering is done before to allow 3D smoothing
		// I movingLow = (I) img.copy();
		// I fixedLow = (I) ref.copy();
		
		I w = null; // (I) wInit.factory().create(wInit);
		I wTmp = null;
		// I wTmp = (I) wInit.factory().create(wInit);
		
		I movingLevel = null; // (I) movingLow.copy();
		I fixedLevel = null; // (I) fixedLow.copy();
		I tmp = null; //  (I) movingLevel.copy();
		
		/* outer loop with the pyramid */
		for (int l = max_level; l >= 0; l--) {
						
			double scalingFactor = Math.pow(eta, l);
			
			int[] levelSize = new int[] {
				(int)(Math.round(scalingFactor * (double)width)), 
				(int)(Math.round(scalingFactor * (double)height))};
		
			int nxLevel = levelSize[0] + 2;
			int nyLevel = levelSize[1] + 2;
			
			movingLevel = Util.resize(img, levelSize);
			fixedLevel = Util.resize(ref, levelSize);
			
			float hx = (float)width / (float)levelSize[0];
			float hy = (float)height / (float)levelSize[1];
			float[] alpha_level = new float[2];
			for (int i = 0; i < 2; i++) {
				alpha_level[i] = alpha[i] / (float)Math.pow(eta, (double)i);
			}
			
			if (l == max_level) {
				wTmp = Util.resize(wInit, levelSize);
				tmp = movingLevel;
			} else {
				wTmp = Util.resize(w, levelSize);
				I wScaled = (I) wTmp.copy();
				
				Util.dividePut(Views.interval(wScaled, 
						new long[] { 0, 0, 0}, 
						new long[] {wScaled.dimension(0)-1, wScaled.dimension(1)-1, 0}), hx);
				Util.dividePut(Views.interval(wScaled, 
						new long[] { 0, 0, 1}, 
						new long[] {wScaled.dimension(0)-1, wScaled.dimension(1)-1, 1}), hy);
				
				tmp = (I) movingLevel.factory().create(movingLevel);
				Util.warp(movingLevel, wScaled, tmp);
			}
			
			wTmp = Util.copyMakeBorder(wTmp, Util.BORDER_CONSTANT);
			I dw = (I) wTmp.factory().create(wTmp);

			float[] du = dw.getPlane(0).getCurrentStorageArray();
			float[] dv = dw.getPlane(1).getCurrentStorageArray();
			
			float[] psi = new float[nxLevel * nyLevel * nChannels];
			float[] psiSmooth = new float[nxLevel * nyLevel];
			for (int i = 0; i < nxLevel * nyLevel; i++) {
				du[i] = 0.0f;
				dv[i] = 0.0f;
				psiSmooth[i] = 1.0f;
			}
			for (int i = 0; i < nxLevel * nyLevel * nChannels; i++) {
				psi[i] = 1.0f;
			}
			
			// preparing the motion tensor:			
			MotionTensor<I> mt = getMotionTensorGC(fixedLevel, tmp, hx, hy);
			
			float[] uPtr = wTmp.getPlane(0).getCurrentStorageArray();
			float[] vPtr = wTmp.getPlane(1).getCurrentStorageArray();

			int iterationCounter = 0;
			/* outer loop: */
		    while (iterationCounter++ < iterations) {
		        
		        if (iterationCounter % updateLag == 0) {
		            nonlinearity(psi, mt, du, dv, nChannels, aData);
					nonlinearitySmoothness(psiSmooth, wTmp, dw, aSmooth, hx, hy);
		        }
	
				/* Gauss Seidel step: */
		        for (int j = 1; j < nyLevel - 1; j++) {
		        	for (int i = 1; i < nxLevel - 1; i++) {
		            
						idx = j * nxLevel + i;
						idxLeft = j * nxLevel + (i - 1);
						idxRight = j * nxLevel + (i + 1);
						idxDown = (j + 1) * nxLevel + i;
						idxUp = (j - 1) * nxLevel + i;

						wRight = i < nxLevel - 1 ? 
		                    0.5f * (psiSmooth[idx]  + psiSmooth[idxRight]) * alpha_level[0] / (hx * hx) : 0;
						wLeft =  i > 1      ? 
		                    0.5f * (psiSmooth[idx]  + psiSmooth[idxLeft]) * alpha_level[0] / (hx * hx) : 0;
						wDown =  j < nyLevel - 1 ? 
		                    0.5f * (psiSmooth[idx]  + psiSmooth[idxDown]) * alpha_level[1] / (hy * hy) : 0;
						wUp =	  j > 1      ? 
		                    0.5f * (psiSmooth[idx]  + psiSmooth[idxUp]) * alpha_level[1] / (hy * hy) : 0;
						wSum = wUp + wDown + wLeft + wRight;

						// summing up the data terms
						denomU = 0;
						denomV = 0;
						numU = 0;
						numV = 0;
						for (int k = 0; k < nChannels; k++) {
							
							ndIdx = j * nxLevel + i + k * (nxLevel * nyLevel);
							
							numU -= weight[k] * psi[ndIdx] * (mt.j13[ndIdx] + mt.j12[ndIdx] * dv[idx]);
	
							denomU += weight[k] * psi[ndIdx] * mt.j11[ndIdx];
							denomV += weight[k]  * psi[ndIdx] * mt.j22[ndIdx];
						}
	
						denomU += wSum;
						denomV += wSum;
	
						du[idx] = (1 - OMEGA) * du[idx] +
							OMEGA * (numU
								+ wLeft * (uPtr[idxLeft] + du[idxLeft])
								+ wUp * (uPtr[idxUp] + du[idxUp])
								+ wDown * (uPtr[idxDown] + du[idxDown])
								+ wRight  * (uPtr[idxRight] + du[idxRight])
								- wSum * (uPtr[idx]))
							/ (denomU);
						
						for (int k = 0; k < nChannels; k++) {
							ndIdx = j * nxLevel + i + k * (nxLevel * nyLevel);
							numV -= weight[k] * psi[ndIdx] * (mt.j23[ndIdx] + mt.j12[ndIdx] * du[idx]);
						}
	
						dv[idx] = (1 - OMEGA) * dv[idx] +
							OMEGA * (numV
								+ wLeft * (vPtr[idxLeft] + dv[idxLeft])
								+ wUp * (vPtr[idxUp] + dv[idxUp])
								+ wDown * (vPtr[idxDown] + dv[idxDown])
								+ wRight * (vPtr[idxRight] + dv[idxRight])
								- wSum * (vPtr[idx]))
							/ (denomV);
					}
				} // GS step
			} // outer loop
		    
		    Util.medianBlur5x5(dw);
		    Util.addPut(wTmp, dw);
		    
		    w = (I) wTmp.factory().create(new long[] {nxLevel-2, nyLevel-2, 2});
		    
		    RandomAccessibleInterval<FloatType> wInner = Views.offsetInterval(wTmp, 
					new long[] { 1, 1, 0}, 
					new long[] {nxLevel-2, nyLevel-2, 1});
		    
		    Cursor<FloatType> wCursor = w.cursor();
		    RandomAccess<FloatType> wInnerAccess = wInner.randomAccess();
		    
		    while (wCursor.hasNext()) {
		    	wCursor.fwd();
		    	wInnerAccess.setPosition(wCursor);
		    	wCursor.get().set(wInnerAccess.get());
		    }
		} // pyramid
		
		return w;
	}
	
	private <I extends PlanarImg<FloatType, FloatArray>> void nonlinearitySmoothness(float[] psiSmooth, 
			I w, I dw, float a, float hx, float hy) {
		
		long width = w.dimension(0);
		long height = w.dimension(1);
		
		I wFull = Util.add(w, dw); 
		
		I wx = Util.imgradientX(wFull, hx);
		I wy = Util.imgradientY(wFull, hy);
		
		float[] ux = wx.getPlane(0).getCurrentStorageArray();
		float[] vx = wx.getPlane(1).getCurrentStorageArray();
		float[] uy = wy.getPlane(0).getCurrentStorageArray();
		float[] vy = wy.getPlane(1).getCurrentStorageArray();
		
		float tmp;
		for (int i = 0; i < height * width; i++) {
			tmp = ux[i] * ux[i] + uy[i] * uy[i] + vx[i] * vx[i] + vy[i] * vy[i];
			tmp = tmp < 0 ? 0 : tmp;
			psiSmooth[i] = a * (float)Math.pow(tmp + eps, a - 1);
		}
	}
	
	private <I extends PlanarImg<FloatType, FloatArray>> void nonlinearity(
			float[] psi, MotionTensor<I> mt, float[] du, float[] dv,
			int n_channels, float[] a) {
		
		float tmp;
		int idx;
		int n = du.length;
		for (int k = 0; k < n_channels; k++) {
			for (int i = 0; i < n; i++) {		
				
				idx = i + k * n;
				tmp = mt.j11[idx] * du[i] * du[i] 
						+ mt.j22[idx] * dv[i] * dv[i] 
						+ mt.j23[idx] * dv[i] +
						+ 2 * mt.j12[idx] * du[i] * dv[i] 
						+ 2 * mt.j13[idx] * du[i] + mt.j23[idx] * dv[i] + mt.j33[idx];
				tmp = tmp < 0 ? 0 : tmp;
				psi[idx] = a[k] * (float) Math.pow(tmp + eps, a[k] - 1);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private <I extends PlanarImg<FloatType, FloatArray>> MotionTensor<I> getMotionTensorGC(
			I img, I ref, float hx, float hy) {
		
		ArrayList<I> J = new ArrayList<I>(6);
		for (int i = 0; i < 6; i++) {
			J.add(i, (I) img.factory().create(img));
		}
		
		I fx = Util.imgradientX(ref, hx);
		I fx2 = Util.imgradientX(img, hx);
		
		Util.addPut(fx, fx2);
		Util.dividePut(fx, 2.0f);
		
		I fy = Util.imgradientY(ref, hy);
		I fy2 = Util.imgradientY(img, hy);
		Util.addPut(fy, fy2);
		Util.dividePut(fy, 2.0f);
		
		I ft = (I) img.copy();
		Util.subtractPut(ft, ref);
		
		I fxy = Util.imgradientY(fx, hy);
		I fxt = Util.imgradientX(ft, hx);
		I fyt = Util.imgradientY(ft, hy);
		
		I fxx = Util.imgradientXX(ref, hx);
		I fxx2 = Util.imgradientXX(img, hx);
		Util.addPut(fxx, fxx2);
		Util.dividePut(fxx, 2);
		
		I fyy = Util.imgradientYY(ref, hy);
		I fyy2 = Util.imgradientYY(img, hy);
		Util.addPut(fyy, fyy2);
		Util.dividePut(fyy, 2);
				
		float[] FX, FY, FXX, FYY, FXY, FXT, FYT, 
			J11, J22, J33, J12, J13, J23;
		float regX, regY, FXXsquared, FYYsquared, FXYsquared;
		float eps = 0.000001f;
		
		for (int i = 0; i < img.numSlices(); i++) {
			FXX = fxx.getPlane(i).getCurrentStorageArray();
			FYY = fyy.getPlane(i).getCurrentStorageArray();
			FXY = fxy.getPlane(i).getCurrentStorageArray();
			FXT = fxt.getPlane(i).getCurrentStorageArray();
			FYT = fyt.getPlane(i).getCurrentStorageArray();
			
			J11 = J.get(0).getPlane(i).getCurrentStorageArray();
			J22 = J.get(1).getPlane(i).getCurrentStorageArray();
			J33 = J.get(2).getPlane(i).getCurrentStorageArray();
			J12 = J.get(3).getPlane(i).getCurrentStorageArray();
			J13 = J.get(4).getPlane(i).getCurrentStorageArray();
			J23 = J.get(5).getPlane(i).getCurrentStorageArray();
			
			for (int j = 0; j < FXX.length; j++) {
				FXXsquared = FXX[j] * FXX[j];
				FYYsquared = FYY[j] * FYY[j];
				FXYsquared = FXY[j] * FXY[j];
				regX = FXXsquared + FXYsquared;
				regY = FXYsquared + FYYsquared;
				regX = regX < 0.0f ? 0.0f : regX;
				regY = regY < 0.0f ? 0.0f : regY;
				regX = 1.0f / ((float)Math.pow(Math.sqrt(regX), 2) + eps);
				regY = 1.0f / ((float)Math.pow(Math.sqrt(regY), 2) + eps);
				regX = 1.0f;
				regY = 1.0f;
				
				J11[j] = regX * FXXsquared + regY * FXYsquared;
				J22[j] = regX * FXYsquared + regY * FYYsquared;
				J33[j] = regX * FXT[j] * FXT[j] + regY * FYT[j] * FYT[j];
				J12[j] = regX * FXX[j] * FXY[j] + regY * FXY[j] * FYY[j];
				J13[j] = regX * FXX[j] * FXT[j] + regY * FXY[j] * FYT[j];
				J23[j] = regX * FXY[j] * FXT[j] + regY * FYY[j] * FYT[j];
			}
		}
		
		for (int i = 0; i < 6; i++) {
			J.set(i, Util.copyMakeBorder(J.get(i), Util.BORDER_CONSTANT));
		}
		
		MotionTensor<I> result = new MotionTensor<I>(J);
	    
		return result;
	}
	
	/**
	 * Utility functions:
	 */
	private int warpingDepth(int height, int width) {
		float min_dim = (float) Math.min(height, width);
		int warpingdepth = 0;
		for (int i = 1; i < levels; i++) {
		    warpingdepth = warpingdepth + 1;
		    min_dim = min_dim * eta;
		    if (Math.round(min_dim) < 10 )
		        break;
		}
		return warpingdepth;
	}

	private class MotionTensor <I extends PlanarImg<FloatType, FloatArray>> {
		public float[] j11;
		public float[] j22;
		public float[] j33;
		public float[] j12;
		public float[] j13;
		public float[] j23;
		
		public ArrayList<I> J;
		
		private int n_channels;
		private int width;
		private int height;
		private int array_size;
		
		public MotionTensor (ArrayList<I> J) {
			this.J = J;
			
			this.n_channels = (int)J.get(0).numSlices();
			this.width = (int) J.get(0).dimension(0);
			this.height = (int) J.get(0).dimension(1);
			
			array_size = n_channels * width * height;
			
			j11 = get_nd_float(J.get(0));
			j22 = get_nd_float(J.get(1));
			j33 = get_nd_float(J.get(2));
			j12 = get_nd_float(J.get(3));
			j13 = get_nd_float(J.get(4));
			j23 = get_nd_float(J.get(5));
		}
		
		private float[] get_nd_float(I j) {
			int nd_idx;
			float[] out = new float[array_size];
			for (int k = 0; k < n_channels; k++) {
				final float[] tmp = j.getPlane(k).getCurrentStorageArray();
				for (int i = 0; i < height * width; i++) {
					nd_idx = i + k * height * width;
					out[nd_idx] = tmp[i];
				}
			}
			return out;
		}
	}
	
	
	private static void demo() {
		
	}
}