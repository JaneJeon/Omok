import MyDataStructures.MyPQ;
import MyDataStructures.SLList;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

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

	@org.junit.Test
	public void SLList() throws Exception {
		SLList foo = new SLList(6);
		foo.add(1);
		foo.add(2);
		foo.add(3);
		foo.add(4);
		foo.add(5);
		assertEquals(5, foo.getSize());
		assertEquals(1, foo.getHeadData());
		assertEquals(5, foo.getTailData());
		foo.add(6);
		assertEquals(6, foo.pop());
		foo.add(66);
		foo.add(77);
		foo.add(88);
		foo.add(99);
		assertEquals(6, foo.getSize());
		assertEquals(4, foo.getHeadData());
		assertEquals(99, foo.getTailData());
		assertEquals(99, foo.pop());
		assertEquals(5, foo.getSize());
		assertEquals(4, foo.getHeadData());
		assertEquals(88, foo.getTailData());
	}

	@org.junit.Test
	public void rotate() throws Exception {
		int[] xy = {0, 1};
		int[] xy2 = {1, 0};
		int[] xy3 = {0, -1};
		assertArrayEquals(xy2, Functions.rotate(xy));
		assertArrayEquals(xy3, Functions.rotate(xy2));
		int[] xy4 = {13, 22};
		int[] xy5 = {22, -13};
		int[] xy6 = {-13, -22};
		assertArrayEquals(xy5, Functions.rotate(xy4));
		assertArrayEquals(xy6, Functions.rotate(xy5));
	}

	@org.junit.Test
	public void reflect() throws Exception {
		assertEquals(new Point(9, 3), Functions.reflect(new Point(5, 7), new Point(6, 4), new Point(9, 7)));
		assertEquals(new Point(18, 8), Functions.reflect(new Point(12, 8), new Point(15, 16), new Point(15, 8)));
		assertEquals(new Point(3, 4), Functions.reflect(new Point(3, 10), new Point(9, 7), new Point(7, 7)));
	}
}