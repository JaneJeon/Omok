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
		// should test time, turn, # of moves till win in addition to winning
		// report format?
	}

	public static void warmup() {
		// simply find the first three best moves
		Jack AI = new Jack(0.92, (double) 2/3, 2, 1, 5, 9);
		AI.addPoint(9, 9);
		AI.addPoint(AI.winningMove().x, AI.winningMove().y);
		AI.addPoint(AI.winningMove().x, AI.winningMove().y);
	}
}