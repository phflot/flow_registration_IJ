package floregistration.algorithm;

/**
 * 
 * Class to store the solver specific parameters
 *
 */
public class RegistrationSolverOptions {
	public int iterations = 50;
	public int updateLag = 5;
	public int levels = 100;
	public float[] alpha = new float[] {1.0f, 1.0f};
	public float[] aData = new float[] {0.5f, 0.5f};
	public float aSmooth = 1.0f;
	public float eta = 0.75f;
	public float sigma = 3.0f;
	
	public RegistrationSolverOptions(int iterations, int updateLag, int levels, float[] alpha, float[] a_data,
			float a_smooth, float eta, float sigma) {
		this.iterations = iterations;
		this.updateLag = updateLag;
		this.levels = levels;
		this.alpha = alpha;
		this.aData = a_data;
		this.aSmooth = a_smooth;
		this.eta = eta;
		this.sigma = sigma;
	}
	
	public RegistrationSolverOptions() {}
}
