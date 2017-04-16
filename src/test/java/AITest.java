import java.awt.*;
import java.util.*;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class AITest {
	private Jack AI;
	private List<Point> points;

	@org.junit.Before
	public void setUp() throws Exception {
		AI = new Jack(1, 0, 2, 1, 5, 15);
		AI.addPoint(9, 9);
		points = new ArrayList<>();
		points.add(new Point(1, 3));
		points.add(new Point(2, 4));
		points.add(new Point(4, 6));
	}

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