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
	public int minLevel = 0;
	public float[] alpha = new float[] {1.5f, 1.5f};
	public float[] aData = new float[] {0.45f, 0.45f};
	public float eta = 0.8f;
	public float sigma = 1.0f;
	
	public RegistrationSolverOptions(int iterations, int updateLag, int levels, float[] alpha, float[] a_data, float eta, float sigma) {
		this.iterations = iterations;
		this.updateLag = updateLag;
		this.levels = levels;
		this.alpha = alpha;
		this.aData = a_data;
		this.eta = eta;
		this.sigma = sigma;
	}
	
	public RegistrationSolverOptions() {}
}
