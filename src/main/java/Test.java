// for testing which versions of AI are the best
public class Test {
	public static void main(String[] args) {
		// variables to vary:
		// 1st: defense weight - 0.1, 0.3, 0.5, 0.7, 0.9
		// 2nd: threshold - 0.33 0.5 0.66 0.75
		// 3rd: M - 2 3 4
		// 4th: clashEvalMethod - 1 2 3
		// 5th: branch limit - 5 4 3
		// once I'm done with all the benchmarks, try varying the depth and the time.
		double[] time = new double[2];
		double[] defense_weight = {0.1, 0.3, 0.5, 0.7, 0.9};
		double[] threshold = {0.33, 0.5, 0.66, 0.75};
		int[] M = {2, 3, 4};
		int[] clashEvalMethod = {1, 2, 3};
		int[] branch_limit = {5, 4, 3};
		// what should I test besides won?
		// time, turns, # of moves till win

		// report format?
		//
	}
}