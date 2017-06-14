import sun.audio.AudioPlayer;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/*
 * @author: Sungil Ahn
 */
public class Test {
	public static void main(String[] args) {
		// for testing which versions of AI are the best
		// variables to vary:
		// 1st: defense weight - 0.1, 0.3, 0.5, 0.7, 0.9
		// 2nd: threshold - 0.33 0.5 0.66 0.75
		// 3rd: M - 2 3 4
		// 4th: clashEvalMethod - 1 2 3
		// 5th: branch limit - 5 4 3
		// once I'm done with all the benchmarks, try varying the depth and the time.
		// should test time, turn, # of moves till win in addition to winning
		// report format?

		firstMoveTest();
	}

	public static void warmup() {
		// start optimizing instantiation first, then gradually ramp up the number of nodes to make the warmup faster
		Jack AI = LoadResource.getAI(true, false, 1).depth(7).branchLImit(6);
		AI.addPoint(9, 9);
		AI.winningMove();
		Jack AI2 = LoadResource.getAI(true, false, 1);
		AI2.addPoint(9, 9);
		AI2.winningMove();
		Jack AI3 = LoadResource.getAI(true, false, 2);
		AI3.addPoint(9, 9);
		AI3.winningMove();
	}
	
	// A manual test, similar to that of the unit test. Used to test the application when I turn off this
	// portion in the unit test to save time on compilation.
	// This will take at least 10 minutes. Go do something else, and upon reaching error or success,
	// a corresponding sound effect will play to notify that the test is done.
	public static void manualTest() {
		int turns = 0;
		double startTime = System.nanoTime();
		Jack AI = new Jack(true).defenseWeight(1).threshold(0).pieceScore(2).evalMethod(1).branchLImit(6).depth(13)
			.debug(false);
		AI.addPoint(9, 9);
		while (!AI.won()) {
			Point p;
			p = AI.winningMove();
			if (p.equals(new Point(50, 50))) {
				System.out.println("Uh oh!");
				double endTime = System.nanoTime();
				int duration = (int) ((endTime - startTime) / 1_000_000_000);
				System.out.println("It took " + duration + " s to fail, with "+turns+" turns");
				AudioPlayer.player.start(LoadResource.getSfx("sfx4.aiff"));
				System.exit(-1);
			}
			turns++;
			AI.addPoint(p.x, p.y);
		}
		double endTime = System.nanoTime();
		int duration = (int) ((endTime - startTime) / 1_000_000_000);
		System.out.println("It took " + duration + " s to pass, with "+turns+" turns");
		AudioPlayer.player.start(LoadResource.getSfx("sfx3.aiff"));
	}

	public static void dateTest() {
		System.out.println(LoadResource.getTime());
	}

	public static void firstMoveTest() {
		int search = 10;
		for (int j = 1; j < 4; j++) {
			Map<Point, Integer> result = new HashMap<>(4);
			int nodes = 0;
			for (int i = 0; i < search; i++) {
				Jack AI = LoadResource.getAI(true, false, j);
				AI.addPoint(9, 9);
				Point best = AI.winningMove();
				result.put(best, result.containsKey(best) ? result.get(best) + 1 : 1);
				nodes += AI.numNodes();
			}
			System.out.println("Case " + j + ":");
			for (Point p : result.keySet())
				System.out.println("Point " + LoadResource.printPoint(p) + " appeared " + result.get(p) + " times.");
			System.out.println("Average number of nodes searched: " + nodes / search);
		}
	}
}