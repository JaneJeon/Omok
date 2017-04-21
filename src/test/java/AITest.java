import java.awt.*;
import java.util.*;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class AITest {
	private Jack AI;
	private List<Point> points;

	// make the AI play against itself
	@org.junit.Before
	public void setUp() throws Exception {
		AI = new Jack(1, 0, 2, 1, 6, 13);
		AI.addPoint(9, 9);
		points = new ArrayList<>();
		points.add(new Point(1, 3));
		points.add(new Point(2, 4));
		points.add(new Point(4, 6));
	}

	// TODO: see if the test really is running after the AI is properly set up...(because manual test is working fine)
	// set to fail when there's errors in score calculations - continue until game ends
//	@org.junit.Test
//	public void winningMove() throws Exception {
//		while (!AI.won()) {
//			Point p;
//			p = AI.winningMove();
//			assertNotEquals(new Point(50, 50), p);
//			AI.addPoint(p.x, p.y);
//		}
//	}
	
	@org.junit.Test
	public void length() throws Exception {
		assertEquals(3, AI.length(points));
	}
}