package floregistration.algorithm;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.convolution.Convolution;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.convolution.kernel.SeparableKernelConvolution;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

import java.util.Arrays;
import java.util.stream.IntStream;

public class Util {
	
	public static final int BORDER_REFLECT_101 = 0;
	public static final int BORDER_CONSTANT = 1;
	
	public static final int INTERP_LINEAR = 0;
	
	private static final Convolution<NumericType<?>> dxConvolver = 
			SeparableKernelConvolution.convolution1d(
					Kernel1D.centralAsymmetric(new double[] {-1.0f, 0.0f, 1.0f}), 0);
	private static final Convolution<NumericType<?>> dyConvolver = 
			SeparableKernelConvolution.convolution1d(
					Kernel1D.centralAsymmetric(new double[] {-1.0f, 0.0f, 1.0f}), 1);
	private static final Convolution<NumericType<?>> dxxConvolver = 
			SeparableKernelConvolution.convolution1d(
					Kernel1D.centralAsymmetric(new double[] {1.0f, -2.0f, 1.0f}), 0);
	private static final Convolution<NumericType<?>> dyyConvolver = 
			SeparableKernelConvolution.convolution1d(
					Kernel1D.centralAsymmetric(new double[] {1.0f, -2.0f, 1.0f}), 1);
	
	// private static Context context = new Context();
	
	/**
	 * function that warps given a input image @param img 
	 * and displacement field @param u, @param v into @param target
	 */
	public static <T extends NumericType<T>, I extends IterableInterval<T> & RandomAccessibleInterval<T>> void 
		warp(I img, PlanarImg<FloatType, FloatArray> w, I target) {
		
		NLinearInterpolatorFactory< T > interpolatorFactory = new NLinearInterpolatorFactory<>();
		RealRandomAccessible<T> interpolator = Views.interpolate(Views.extendZero(img), interpolatorFactory);
		
		int width = (int)img.dimension(0);
		
		Cursor<T> inputCursor = img.cursor();
		Cursor<T> outputCursor = target.cursor();
		
		RealRandomAccess<T> rra = interpolator.realRandomAccess();
		
		float[] uArr = w.getPlane(0).getCurrentStorageArray();
		float[] vArr = w.getPlane(1).getCurrentStorageArray();
		
		while (inputCursor.hasNext()) {
			inputCursor.fwd();
			outputCursor.fwd();
			
			int xPos = inputCursor.getIntPosition(0);
			int yPos = inputCursor.getIntPosition(1);
			int idx = yPos * width + xPos;
			
			rra.setPosition(new double[]
					{ (double)xPos + (double)uArr[idx],
					  (double)yPos + (double)vArr[idx],
					  inputCursor.getDoublePosition(2)							
					});
			outputCursor.get().set(rra.get());
		}
	}
	
	/**
	 * Functions to handle any imp file type
	 */
	public static <T extends NativeType<T> & ComplexType<T>, I extends IterableInterval<T> & RandomAccessibleInterval<T>>
		ImagePlusImg<FloatType, FloatArray> imgToFloat(I img) {
		
		Cursor<T> inCursor = img.cursor();
		
		long[] dims = new long[img.numDimensions()];
		img.dimensions(dims);
		
		@SuppressWarnings("unchecked")
		ImagePlusImg<FloatType, FloatArray> out = (ImagePlusImg<FloatType, FloatArray>) 
			new ImagePlusImgFactory<FloatType>(new FloatType()).create(dims);
		
		Cursor<FloatType> outCursor = out.cursor();
		
		while(inCursor.hasNext()) {
			inCursor.fwd();
			outCursor.fwd();
			
			outCursor.get().setReal(inCursor.get().getRealFloat());
		}
		
		return out;
	}
	
	
	public static <T extends NativeType<T> & ComplexType<T>, I extends IterableInterval<T> & RandomAccessibleInterval<T>>
		ImagePlusImg<FloatType, FloatArray> imgToFloatNormalize(I img) {
	
		return imgToFloatNormalize(img, (float)getMin(img), (float)getMax(img));
	}
	
	
	public static <T extends NativeType<T> & ComplexType<T>, I extends IterableInterval<T> & RandomAccessibleInterval<T>>
		ImagePlusImg<FloatType, FloatArray> imgToFloatNormalize(I img, float minValue, float maxValue) {
		
		Cursor<T> inCursor = img.cursor();
		
		long[] dims = new long[img.numDimensions()];
		img.dimensions(dims);
		
		@SuppressWarnings("unchecked")
		ImagePlusImg<FloatType, FloatArray> out = (ImagePlusImg<FloatType, FloatArray>) 
			new ImagePlusImgFactory<FloatType>(new FloatType()).create(dims);
		
		Cursor<FloatType> outCursor = out.cursor();
		
		while(inCursor.hasNext()) {
			inCursor.fwd();
			outCursor.fwd();
			
			outCursor.get().setReal((inCursor.get().getRealFloat() - minValue) / (maxValue - minValue));
		}
		
		return out;
	}
	
	public static <T extends ComplexType<T>, I extends IterableInterval<T>>
		float getMin(I img) {
		
		Cursor<T> inCursor = img.cursor();
		
		float min = Float.MAX_VALUE;
		
		while(inCursor.hasNext()) {
			inCursor.fwd();
			
			float tmp = inCursor.get().getRealFloat();
			min = tmp < min ? tmp : min;
		}
		
		return min;
	}
	
	public static <T extends ComplexType<T>, I extends IterableInterval<T>>
		float getMax(I img) {
		
		Cursor<T> inCursor = img.cursor();
		
		float max = Float.MIN_VALUE;
		
		while(inCursor.hasNext()) {
			inCursor.fwd();
			
			float tmp = inCursor.get().getRealFloat();
			max = tmp > max ? tmp : max;
		}
		
		return max;
	}
	
	
	public static <T extends NativeType<T> & ComplexType<T>, I extends IterableInterval<T> & RandomAccessibleInterval<T>>
	ImagePlusImg<FloatType, FloatArray> getMean3(I img) {
		ImagePlusImg<FloatType, FloatArray> tmp = imgToFloat(img);
		ImagePlusImg<FloatType, FloatArray> result = getMean3(tmp, 0, tmp.numSlices());
		tmp.close();
		return result;
	}
	
	public static <T extends NativeType<T> & ComplexType<T>, I extends IterableInterval<T> & RandomAccessibleInterval<T>>
	ImagePlusImg<FloatType, FloatArray> getMean3(I img, int low, int high) {
		ImagePlusImg<FloatType, FloatArray> tmp = imgToFloat(img);
		ImagePlusImg<FloatType, FloatArray> result = getMean3f(tmp, low, high);
		tmp.close();
		return result;
	}
	
	
	public static <I extends PlanarImg<FloatType, FloatArray>> I getMean3f(I img) {	
		return getMean3f(img, 0, img.numSlices() - 1);
	}
	
	public static <I extends PlanarImg<FloatType, FloatArray>> I getMean3f(I img, int low, int high) {

		@SuppressWarnings("unchecked")
		I output = (I)img.factory().create(img.dimension(0), img.dimension(1));
		float[] data_array = output.getPlane(0).getCurrentStorageArray();
		for (int i = 0; i < data_array.length; i++)
			data_array[i] = 0.0f;
		
		assert low >= 0;
		assert high < img.numSlices();
		
		IntStream.rangeClosed(low, high).forEach(n -> {
			float[] tmp = img.getPlane(n).getCurrentStorageArray();
			for (int i = 0; i < data_array.length; i++)
				data_array[i] += tmp[i];
		});
		
		for (int i = 0; i < data_array.length; i++)
			data_array[i] /= (float)high - (float)low + 1.0f;
		
		return output;
	}
	
	
	/**
	 * Set of functions that can be used similar to same named opencv functions
	 */	
	@SuppressWarnings("unchecked")
	public static <I extends PlanarImg<FloatType, FloatArray>> I 
		resize(I img, double scalingFactor) {
	
		long[] dims = new long[img.numDimensions()];
		img.dimensions(dims);
		
		for (int i = 0; i < dims.length - 1; i++) {
			System.out.println("Scaling Factor: " + scalingFactor + " dims: " + dims[i]);
			dims[i] = (long)Math.round((double)dims[i] * scalingFactor);
			System.out.println(dims[i]);
		}
		
		I out = (I) img.factory().create(dims);
		
		resize(img, out, scalingFactor, scalingFactor);
		return out;
	}
	
	@SuppressWarnings("unchecked")
	public static <I extends PlanarImg<FloatType, FloatArray>> I 
		resize(I img, int[] dims) {
		
		int[] dimsFinal = dims;
		if (dims.length < img.numDimensions()) {
			dimsFinal = new int[img.numDimensions()];
			for (int i = 0; i < dimsFinal.length; i++) {
				if (i < dims.length)
					dimsFinal[i] = dims[i];
				else
					dimsFinal[i] = (int)img.dimension(i);
			}
		}
		
		I out = (I) img.factory().create(dimsFinal);
		double scalingFactorX = (double)dims[0] / (double)img.dimension(0);
		double scalingFactorY = (double)dims[1] / (double)img.dimension(1);
		
		resize(img, out, scalingFactorX, scalingFactorY);		
		
		return out;
	}
	
	
	@SuppressWarnings("unchecked")
	public static <T extends NumericType<T>, I extends Img<T>> void 
		resize(I img, I target, double scalingFactorX, double scalingFactorY) {
		
		long[] dims = new long[img.numDimensions()];
		img.dimensions(dims);
		I imgLowpass = (I) img.copy();
		
		double sigmaX = 0.0;
		if (scalingFactorX < 1)
			sigmaX = 1 / (2 * scalingFactorX);
		double sigmaY = 0.0;
		if (scalingFactorY < 1)
			sigmaY = 1 / (2 * scalingFactorY);
		Gauss3.gauss(new double[] {sigmaX, sigmaY}, Views.extendBorder(img), imgLowpass);
		
		NLinearInterpolatorFactory< T > interpolatorFactory = new NLinearInterpolatorFactory<>();
		RealRandomAccessible<T> interpolator = Views.interpolate(Views.extendZero(imgLowpass), interpolatorFactory);
		
		Cursor<T> outputCursor = target.cursor();
		
		RealRandomAccess<T> rra = interpolator.realRandomAccess();
		
		while (outputCursor.hasNext()) {
			outputCursor.fwd();
			
			double xPos = ((double)outputCursor.getIntPosition(0)) / scalingFactorX;
			double yPos = ((double)outputCursor.getIntPosition(1)) / scalingFactorY;
			
			rra.setPosition(new double[]
					{ xPos, yPos, outputCursor.getDoublePosition(2)							
					});
			
			outputCursor.get().set(rra.get());
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <I extends PlanarImg<FloatType, FloatArray>> I copyMakeBorder(I img, int border) {
		
		long width = img.dimension(0);
		long height = img.dimension(1);
		int nChannels = img.numSlices();
		
		I out = (I) img.factory().create(width + 2, height + 2, nChannels);
		
		copyMakeBorder(img, out, border);
		
		return out;
	}
	
	public static <T extends NumericType<T>, I extends IterableInterval<T> & RandomAccessibleInterval<T>> void 
		copyMakeBorder(I img, I target, int border) {
		
		ExtendedRandomAccessibleInterval<T, I> imgIn = Views.extendZero(img);		
		switch(border) {
		case BORDER_REFLECT_101:
			imgIn = Views.extendMirrorSingle(img);
			break;
		}

		RandomAccess<T> ra = imgIn.randomAccess();
		Cursor<T> cursor = target.localizingCursor();
		
		while(cursor.hasNext()) {
			cursor.fwd();
			ra.setPosition(new int[] { 
				cursor.getIntPosition(0) - 1, 
				cursor.getIntPosition(1) - 1, 
				cursor.getIntPosition(2) });
			cursor.get().set(ra.get());
		}
	}
	
	public static <T extends FloatType, I extends IterableInterval<T> & RandomAccessibleInterval<T>>
		void dividePut(I img, float a) {
		
		Cursor<T> cursor = img.cursor();
		
		while (cursor.hasNext()) {
			cursor.fwd();
			T tmp = cursor.get();
			tmp.set(tmp.getRealFloat() / a);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends NumericType<T>, I extends Img<T>> I add(I img, I img2) {
		
		I out = (I) img.factory().create(img);
		addPut(out, img2);
		return out;
	}
	
	/**
	 * adds the two images into the first
	 * @param <T>
	 * @param <I>
	 * @param img
	 * @param img2
	 */
	public static <T extends NumericType<T>, I extends IterableInterval<T> & RandomAccessibleInterval<T>>
		void addPut(I img, I img2) {
		
		Cursor<T> cursor = img.cursor();
		Cursor<T> cursor2 = img2.cursor();
		
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor2.fwd();
			T tmp = cursor.get();
			tmp.add(cursor2.get());
		}
	}
	

	/**
	 * subtracts the two images into the first
	 * @param <T>
	 * @param <I>
	 * @param img
	 * @param img2
	 */
	public static <T extends NumericType<T>, I extends IterableInterval<T> & RandomAccessibleInterval<T>>
		void subtractPut(I img, I img2) {
		
		Cursor<T> cursor = img.cursor();
		Cursor<T> cursor2 = img2.cursor();
		
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor2.fwd();
			T tmp = cursor.get();
			tmp.sub(cursor2.get());
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <I extends PlanarImg<FloatType, FloatArray>> I medianBlur5x5(I img) {
		/*
		 * 5x5 median filter
		 */
		
		I out = (I) img.factory().create(img);
		
		for (int c = 0; c < img.numSlices(); c++) {
			
			float[] f = img.getPlane(c).getCurrentStorageArray();
			float[] out_array = out.getPlane(c).getCurrentStorageArray();
			float[] tmp = new float[25];
			int nx = (int) img.dimension(0);
			int ny = (int) img.dimension(1);
			int idx, idx_kernel;
			
			for (int j = 2; j < ny - 2; j++) {
				for (int i = 2; i < nx - 2; i++) {
					idx = j * nx  + i;
					for (int iy = -2; iy <= 2; iy++) {
						for (int ix = -2; ix <= 2; ix++) {
							idx_kernel = (j + iy) * nx  + i + ix;
							tmp[(ix + 2) * 5 + iy + 2] = f[idx_kernel];
						}
					}
					Arrays.sort(tmp);
					out_array[idx] = tmp[12];
				}
			}
		}
		return out;
	}
	

	@SuppressWarnings("unchecked")
	public static <I extends PlanarImg<FloatType, FloatArray>> I imgradientX(I img, float hx) {
		I out = (I) img.factory().create(img);
		imgradientX(img, out, hx);
		return out;
	}
	
	public static <T extends FloatType, I extends Img<T>> void imgradientX(
			RandomAccessibleInterval<T> in, I out, float hx) {
		
		dxConvolver.process(Views.extendMirrorSingle(in), out);
		
		Cursor<T> cursor = out.cursor();
		float tmp = 1.0f/(2.0f * hx);
		while(cursor.hasNext()) {
			cursor.fwd();
			cursor.get().mul(tmp);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <I extends PlanarImg<FloatType, FloatArray>> I imgradientY(I img, float hy) {
		I out = (I) img.factory().create(img);
		imgradientY(img, out, hy);
		return out;
	}
	public static <T extends NumericType<?>, I extends Img<FloatType>> void imgradientY(
			RandomAccessibleInterval<T> in, I out, float hy) {
		
		dyConvolver.process(Views.extendMirrorSingle(in), out);
		
		Cursor<FloatType> cursor = out.cursor();
		float tmp = 1.0f/(2.0f * hy);
		while(cursor.hasNext()) {
			cursor.fwd();
			cursor.get().mul(tmp);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <I extends PlanarImg<FloatType, FloatArray>> I imgradientXX(I img, float hx) {
		I out = (I) img.factory().create(img);
		imgradientXX(img, out, hx);
		return out;
	}
	
	public static <T extends NumericType<?>, I extends Img<FloatType>> void imgradientXX(
			RandomAccessibleInterval<T> in, I out, float hx) {
		
		dxxConvolver.process(Views.extendMirrorSingle(in), out);
		
		Cursor<FloatType> cursor = out.cursor();
		float tmp = 1.0f / (hx * hx);
		while(cursor.hasNext()) {
			cursor.fwd();
			cursor.get().mul(tmp);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <I extends PlanarImg<FloatType, FloatArray>> I imgradientYY(I img, float hy) {
		I out = (I) img.factory().create(img);
		imgradientYY(img, out, hy);
		return out;
	}
	
	public static <T extends NumericType<?>, I extends Img<FloatType>> void imgradientYY(
			RandomAccessibleInterval<T> in, I out, float hx) {
		
		dyyConvolver.process(Views.extendMirrorSingle(in), out);
		
		Cursor<FloatType> cursor = out.cursor();
		float tmp = 1.0f / (hx * hx);
		while(cursor.hasNext()) {
			cursor.fwd();
			cursor.get().mul(tmp);
		}
	}
	
	public static double getMinF(float[] inputArray) {
		double minVal = Double.MAX_VALUE;
		for (int i = 0; i < inputArray.length; i++)
			minVal = (double)inputArray[i] < minVal ? (double)inputArray[i] : minVal;
		return minVal;
	}
	
	public static double getMaxF(float[] inputArray) {
		double maxVal = Double.MIN_VALUE;
		for (int i = 0; i < inputArray.length; i++)
			maxVal = inputArray[i] > maxVal ? inputArray[i] : maxVal;
		return maxVal;
	}
	
	/**
	 * Utility functions for vector fields:
	 */
	public static <T extends NumericType<?>, I extends PlanarImg<FloatType, FloatArray>> float getMeanMagnitude(I w) {
		
		float[] u = w.getPlane(0).getCurrentStorageArray();
		float[] v = w.getPlane(1).getCurrentStorageArray();
		
		float tmp;
		float sum = 0;
		for (int i = 0; i < u.length; i++) {
			tmp = u[i] * u[i] + v[i] * v[i];
			tmp = tmp < 0 ? 0 : tmp;
			tmp = (float) Math.sqrt(tmp);
			sum += tmp;
		}	
		
		return (float) sum / (float)u.length;
	}
	
	public static <T extends NumericType<?>, I extends PlanarImg<FloatType, FloatArray>> float getMaxMagnitude(I w) {

		float[] u = w.getPlane(0).getCurrentStorageArray();
		float[] v = w.getPlane(1).getCurrentStorageArray();
		
		float tmp;
		float maxMag = Float.MIN_VALUE;
		for (int i = 0; i < u.length; i++) {
			tmp = u[i] * u[i] + v[i] * v[i];
			tmp = tmp < 0 ? 0 : tmp;
			tmp = (float) Math.sqrt(tmp);
			maxMag = tmp > maxMag ? tmp : maxMag;
		}	
		
		return maxMag;
	}
	
	public static <T extends NumericType<?>, I extends PlanarImg<FloatType, FloatArray>> float getMeanDivergence(I w) {
		
		I wx = imgradientX(w, 1);
		I wy = imgradientY(w, 1);
		
		float[] ux = wx.getPlane(0).getCurrentStorageArray();
		float[] vy = wy.getPlane(1).getCurrentStorageArray();
		
		float div = 0;
		for (int i = 0; i < ux.length; i++) {
			div -= ux[i] + vy[i];
		}
		
		return (float) div / (float)ux.length;
	}
	
	public static class StopwatchTimer {
		private long startTime;
		
		public StopwatchTimer() {
			this.tic();
		}
		
		public void tic() {
			startTime = System.currentTimeMillis();
		}
		
		public double toc() {
			long elapsed = System.currentTimeMillis() - startTime;
			return (double)elapsed / 1000; 
		}
		
		public void tocMsg(String msg) {
			System.out.println(msg + toc() + "s");
		}
	}

}