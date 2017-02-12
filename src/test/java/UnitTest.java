import java.awt.*;

import static org.junit.Assert.*;

public class UnitTest {
	private Jack AI1;
	private Jack AI2;
	private int turn;

	@org.junit.Before
	public void setUp() throws Exception {
		turn = 1;
		AI1 = new Jack(0, 0, 2, 1, 5, 13);
		AI2 = new Jack(1, 0, 3, 1, 4, 15);
		AI1.addPoint(9, 9);
		AI2.addPoint(9, 9);
		turn = -turn;
	}

	@org.junit.Test
	public void winningMove() throws Exception {
		while (!(AI1.won() || AI2.won())) {
			Point p;
			p = turn == 1 ? AI1.winningMove() : AI2.winningMove();
			assertNotEquals(new Point(50, 50), p);
			AI1.addPoint(p.x, p.y);
			AI2.addPoint(p.x, p.y);
		}
	}
}