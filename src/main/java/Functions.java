/*
 * @author: Sungil Ahn
 */

import java.awt.*;

public class Functions {
	/*
 	 * Rotating Δx & Δy around clockwise by 90 degrees doing matrix multiplication from the left:
 	 * 
 	 * | 0 1 | . | Δx | = | Δx' |
 	 * |-1 0 |   | Δy |   | Δy' |
 	 * ^ imagine that these are matrices
 	 * 
 	 * @input: int array of size 2 (Δx, Δy)
 	 * @output: int array of size 2 (Δx', Δy')
 	 */
	public int[] rotate(int[] delta) {
		int[] result = new int[2];
		result[0] = delta[1];
		result[1] = -delta[0];
		return result;
	}

	/*
	 * Returns a reflection of point p over an axis formed by p1 and p2
	 * See here for details: http://stackoverflow.com/questions/3306838/algorithm-for-reflecting-a-point-across-a-line
	 */
	public Point reflect(Point p, Point p1, Point p2) {
		int m = (p2.y - p1.y) / (p2.x - p1.x); // getting the slope of the axis
		int c = p1.y - m * p1.x; // y = mx + c
		int d = (p.x + (p.y - c) * m) / (1 + m * m); // the magic factor
		return new Point(2 * d - p.x, 2 * m * d - p.y + 2 * c);
	}
}