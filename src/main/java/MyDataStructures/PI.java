package MyDataStructures;

import java.awt.*;
import java.io.Serializable;

/*
 * @author: Sungil Ahn
 */

// custom data type for holding point and int
public class PI implements Serializable {
	private static final long serialVersionUID = 135792036854775807L;

	/**
	 * @serial
	 */
	private final Point p;
	/**
	 * @serial
	 */
	private int i;

	public PI(Point p, int i) {
		this.p = p; this.i =i;
	}

	public Point getP() {
		return p;
	}

	public int getI() {
		return i;
	}

	public void setI(int i) {
		this.i = i;
	}

	public String toString() {
		return "<(" + p.x + ", " + p.y + "), " + i + ">";
	}
}