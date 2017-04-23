import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import java.awt.*;
import java.io.IOException;

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
		
		manualTest();
	}

	public static void warmup() {
		// simply find the first three best moves
		Jack AI = new Jack(0.95, 0.6, 2, 1, 5, 11, false);
		AI.addPoint(9, 9);
		AI.winningMove();
		Jack AI2 = new Jack(0.9, 0.6, 2, 1, 6, 9, false);
		AI2.addPoint(9, 9);
		AI2.winningMove();
	}
	
	// A manual test, similar to that of the unit test. Used to test the application when I turn off this
	// portion in the unit test to save time on compilation.
	// This will take at least 10 minutes. Go do something else, and upon reaching error or success,
	// a corresponding sound effect will play to notify that the test is done.
	public static void manualTest() {
		int turns = 0;
		double startTime = System.nanoTime();
		Jack AI = new Jack(1, 0, 2, 1, 6, 13, true);
		AI.addPoint(9, 9);
		while (!AI.won()) {
			Point p;
			p = AI.winningMove();
			if (p.equals(new Point(50, 50))) {
				System.out.println("Uh oh!");
				double endTime = System.nanoTime();
				int duration = (int) ((endTime - startTime) / 1_000_000_000);
				System.out.println("It took " + duration + " s to fail, with "+turns+" turns");
				try {
					AudioPlayer.player.start(new AudioStream(Test.class.getClassLoader().getResourceAsStream("sfx4.aiff")));
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.exit(-1);
			}
			turns++;
			AI.addPoint(p.x, p.y);
		}
		double endTime = System.nanoTime();
		int duration = (int) ((endTime - startTime) / 1_000_000_000);
		System.out.println("It took " + duration + " s to pass, with "+turns+" turns");
		try {
			AudioPlayer.player.start(new AudioStream(Test.class.getClassLoader().getResourceAsStream("sfx3.aiff")));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}