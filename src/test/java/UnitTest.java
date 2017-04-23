import MyDataStructures.MyPQ;
import MyDataStructures.PI;

import java.awt.*;
import java.util.*;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/*
 * @author: Sungil Ahn
 */
public class UnitTest {
	private Jack AI;
	private List<Point> points;
	private MyPQ pq;

	// make the AI play against itself
	@org.junit.Before
	public void setUp() throws Exception {
		AI = new Jack(1, 0, 2, 1, 6, 13, true);
		AI.addPoint(9, 9);
		points = new ArrayList<>();
		points.add(new Point(1, 3));
		points.add(new Point(2, 4));
		points.add(new Point(4, 6));
		
		pq = new MyPQ(5, false);
		pq.push(5, new Point(5, 5));
		pq.push(-5, new Point(5, 6));
		pq.push(4, new Point(4, 4));
		pq.push(-4, new Point(4, 5));
		pq.push(-3, new Point(3, 3));
		pq.push(5, new Point(5, 7));
		pq.push(-6, new Point(6, 6));
	}

	// set to fail when there's errors in score calculations - continue until game ends
	// just like with the manual test, expect this to take about 10 minutes
	@org.junit.Test
	public void winningMove() throws Exception {
		while (!AI.won()) {
			Point p;
			p = AI.winningMove();
			assertNotEquals(new Point(50, 50), p);
			AI.addPoint(p.x, p.y);
		}
	}
	
	@org.junit.Test
	public void length() throws Exception {
		assertEquals(3, AI.length(points));
	}

	@org.junit.Test
	public void pq() throws Exception {
		assertEquals(6, Math.abs(pq.peek()));
		pq.pop();
		assertEquals(5, Math.abs(pq.peek()));
		pq.pop();
		assertEquals(5, Math.abs(pq.peek()));
		pq.pop();
		assertEquals(5, Math.abs(pq.peek()));
		pq.pop();
		assertEquals(2, pq.numLowest());
		Point p1 = pq.pop();
		Point p2 = pq.pop();
		assertNotEquals(p1, p2);
		assertEquals(p1.x, p2.x);
	}
}