package Utils;

import MyDataStructures.PI;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * @author: Sungil Ahn
 */
// bunch of helper methods, for use in reading from the book and for AI purposes
// TODO: ensure that the points do indeed exist when reading from the book
// TODO: optimize/clean up some of these helper functions
public class Functions {
	/**
 	 * Rotating Δx & Δy around clockwise by 90 degrees doing matrix multiplication from the left:
 	 * 
 	 * | 0 1 | * | Δx | = | Δx' |
 	 * |-1 0 |   | Δy |   | Δy' |
 	 * ^ imagine that these are matrices
 	 * 
 	 * @input: int array of size 2 (Δx, Δy)
 	 * @output: int array of size 2 (Δx', Δy')
 	 */
	public static int[] rotate(int[] delta) {
		int[] result = new int[2];
		result[0] = delta[1];
		result[1] = -delta[0];
		return result;
	}

	/**
	 * Returns a reflection of point p over an axis formed by p1 and p2
	 * See here for details: http://stackoverflow.com/questions/3306838/algorithm-for-reflecting-a-point-across-a-line
	 */
	public static Point reflect(Point p, Point p1, Point p2) {
		// return early to avoid dividing by zero!
		if (p1.x == p2.x) return new Point(2 * p1.x - p.x, p.y);

		int m = (p2.y - p1.y) / (p2.x - p1.x); // getting the slope of the axis
		int c = p1.y - m * p1.x; // y = mx + c
		int d = (p.x + (p.y - c) * m) / (1 + m * m); // the magic factor
		return new Point(2 * d - p.x, 2 * m * d - p.y + 2 * c);
	}

	/**
	 * Finds the relative position of the threat from the threatSpace
	 */
	public static int[] threatSpaceFinder(Point threat, Point threatSpace) {
		int[] result = new int[2];
		if (threat.x > threatSpace.x) {
			// to the left
			if (threat.y > threatSpace.y) {
				// NW
				result[0] = 5;
			} else if (threat.y < threatSpace.y) {
				// SW
				result[0] = 3;
			} else {
				// left
				result[0] = 4;
			}
			result[1] = threat.x - threatSpace.x - 1;
		} else if (threat.x < threatSpace.x) {
			// to the right
			if (threat.y > threatSpace.y) {
				// NE
				result[0] = 7;
			} else if (threat.y < threatSpace.y) {
				// SE
				result[0] = 1;
			} else {
				// right
				result[0] = 0;
			}
			result[1] = threatSpace.x - threat.x - 1;
		} else {
			if (threat.y > threatSpace.y) {
				// up top
				result[0] = 6;
			} else {
				// down low
				result[0] = 2;
			}
			result[1] = Math.abs(threatSpace.y - threat.y) - 1;
		}
		return result;
	}

	/**
	 * returns true if new point is closer than existing point to threat space
	 */
	public static boolean closer(Point origin, Point existing, int x, int y) {
		return (origin.x - x) * (origin.x - x) + (origin.y - y) * (origin.y - y) <
			(origin.x - existing.x) * (origin.x - existing.x) + (origin.y - existing.y) * (origin.y - existing.y);
	}

	/**
	 * checks if three points are in line
	 */
	public static boolean inLine(Point A, Point B, Point C) {
		return A.x * (B.y - C.y) + B.x * (C.y - A.y) + C.x * (A.y - B.y) == 0;
	}
	
	/**
	 * Checks if a point is within range of 4
	 */
	public static boolean inRange(Point o, Point toCheck) {
		return toCheck.x >= o.x - 4 && toCheck.x <= o.x + 4 && toCheck.y >= o.y - 4 && toCheck.y <= o.y + 4;
	}
	
	/**
	 * returns relative position of latest point in relation to existing sequence
	 */
	public static int position(java.util.List<Point> sequence, Point latestPoint, int length) {
		// should now directly return the number which should be put in the sequence
		Point start = sequence.get(0), end = sequence.get(sequence.size() - 1);
		int xt = (end.x - start.x) / length, yt = (end.y - start.y) / length;
		if (xt != 0) {
			int absPosition = (latestPoint.x - start.x) / xt;
			if (absPosition < 0) {
				return 0;
			} else if (absPosition > length) {
				return sequence.size();
			} else {
				int relPosition = 1;
				while (absPosition > (sequence.get(relPosition).x - start.x) / xt) {
					relPosition++;
				}
				return relPosition;
			}
		} else {
			// only deal with yt
			int absPosition = (latestPoint.y - start.y) / yt;
			if (absPosition < 0) {
				return 0;
			} else if (absPosition > length) {
				return sequence.size();
			} else {
				int relPosition = 1;
				while (absPosition > (sequence.get(relPosition).y - start.y) / yt) {
					relPosition++;
				}
				return relPosition;
			}
		}
	}
	
	/**
	 * checks if there are no opposing colors between the end point and the new point
	 */
	public static boolean isClear(Point end, Point latestPoint, int turn, int[][] board) {
		if (end.x == latestPoint.x) {
			// vertical
			for (int i = 1; i < Math.abs(end.y - latestPoint.y); i++) {
				if (board[end.x][Math.min(end.y, latestPoint.y) + i] == turn) return false;
			}
		} else if (end.y == latestPoint.y) {
			// horizontal
			for (int i = 1; i < Math.abs(end.x - latestPoint.x); i++) {
				if (board[Math.min(end.x, latestPoint.x) + i][end.y] == turn) return false;
			}
		} else if ((end.y - latestPoint.y)/(end.x - latestPoint.x) == 1){
			// right up diagonal (slope = 1)
			for (int i = 1; i < Math.abs(end.x - latestPoint.x); i++) {
				if (board[Math.min(end.x, latestPoint.x) + i][Math.min(end.y, latestPoint.y) + i] == turn) return false;
			}
		} else {
			// right down diagonal (slope = -1)
			for (int i = 1; i < Math.abs(end.x - latestPoint.x); i++) {
				if (board[Math.min(end.x, latestPoint.x) + i][Math.max(end.y, latestPoint.y) - i] == turn) return false;
			}
		}
		return true;
	}
	
	/**
	 * given a sequence in which at least one element is out of range and a point, splits off a new sequence
	 * the resulting sequence is ordered closest from the new point to the farthest
	 */
	public static List<Point> splitOff(Point latestPoint, List<Point> sequence) {
		List<Point> result = new ObjectArrayList<>(4);
		result.add(latestPoint);
		Map<Integer, Point> distance = new Int2ObjectOpenHashMap<>();
		for (Point p : sequence) {
			distance.put((p.x - latestPoint.x) * (p.x - latestPoint.x) +
				(p.y - latestPoint.y) * (p.y - latestPoint.y), p);
		}
		for (int i=1; i<5; i++) {
			for (Integer d : distance.keySet()) {
				if (d == i * i || d == i * i * 2) {
					result.add(distance.get(d));
				}
			}
		}
		return result;
	}
	
	/**
	 * lists all points in the sequence that are on the opposite side of threat space, with latest point as axis
	 * also, can assume that the sequence and point are in line
	 */
	public static List<Point> oppositeSide(Point latestPoint, Point threatSpace, List<Point> sequence) {
		List<Point> result = new ObjectArrayList<>(4);
		if (latestPoint.x == threatSpace.x) {
			// vertical
			if (latestPoint.y > threatSpace.y) {
				for (Point p : sequence) {
					if (p.y > latestPoint.y) result.add(p);
				}
			} else {
				for (Point p : sequence) {
					if (p.y < latestPoint.y) result.add(p);
				}
			}
		} else if (latestPoint.x > threatSpace.x) {
			for (Point p : sequence) {
				if (p.x > latestPoint.x) result.add(p);
			}
		} else {
			for (Point p : sequence) {
				if (p.x < latestPoint.x) result.add(p);
			}
		}
		return result;
	}
	
	/**
	 * returns length of a threat sequence - not the number of pieces, but rather the physical space it takes up
	 * input is ordered list of points
	 */
	public static int length(List<Point> threatSequence) {
		Point start = threatSequence.get(0), end = threatSequence.get(threatSequence.size() - 1);
		int len2 = (start.x - end.x) * (start.x - end.x) + (start.y - end.y) * (start.y - end.y);
		int len1 = (int)Math.sqrt(len2);
		if (len2 == len1 * len1) {
			return len1;
		} else {
			return (int)Math.sqrt(len2 / 2);
		}
	}
	
	/**
	 * given a sequence, checks whether either end is blocked by wall or different color
	 * never, **never** pass in sequences of size 1!
	 */
	public static boolean blocked(List<Point> sequence, int[][] board) {
		int baseColor = board[sequence.get(0).x][sequence.get(0).y];
		int last = sequence.size() - 1;
		int end1x = sequence.get(0).x + (sequence.get(0).x - sequence.get(last).x) / last;
		int end1y = sequence.get(0).y + (sequence.get(0).y - sequence.get(last).y) / last;
		if (!inBoard(end1x, end1y)) return true;
		int end2x = sequence.get(last).x + (sequence.get(last).x - sequence.get(0).x) / last;
		int end2y = sequence.get(last).y + (sequence.get(last).y - sequence.get(0).y) / last;
		return !inBoard(end2x, end2y) || board[end1x][end1y] == -baseColor || board[end2x][end2y] == -baseColor;
	}

	public static boolean inBoard(int x, int y) {
		return !(x > 18 || x < 0 || y > 18 || y < 0);
	}
	
	/**
	 * board eval function - simply add up all the scores on the board
	 */
	public static int total(int[][] array) {
		int total = 0;
		for (int i=0; i<19; i++) {
			for (int j=0; j<19; j++) {
				if (array[i][j] != 0) total += array[i][j];
			}
		}
		return total;
	}

	/**
	 * Reading from a "book"
	 */
	public static List<Point> readBook(Point first, Map<Point, List<List<PI>>> threatSpaces) {
		List<Point> result = new ObjectArrayList<>();
		if (first.x <= 9) {
			// left side of the board
			if (first.y <= 9) {
				// quadrant 2
				for (int i=0; i<2; i++) {
					// adding diagonals
					result.add(threatSpaces.get(first).get(1).get(i).getP());
				}
				if (first.x > first.y) {
					for (int i=0; i<2; i++) {
						result.add(threatSpaces.get(first).get(2).get(i).getP());
					}
				} else {
					for (int i=0; i<2; i++) {
						result.add(threatSpaces.get(first).get(0).get(i).getP());
					}
				}
			} else {
				// quadrant 3
				for (int i=0; i<2; i++) {
					result.add(threatSpaces.get(first).get(7).get(i).getP());
				}
				if (first.x > 19 - first.y) {
					for (int i=0; i<2; i++) {
						result.add(threatSpaces.get(first).get(6).get(i).getP());
					}
				} else {
					for (int i=0; i<2; i++) {
						result.add(threatSpaces.get(first).get(0).get(i).getP());
					}
				}
			}
		} else {
			// right side of the board
			if (first.y <= 9) {
				// quadrant 1
				for (int i=0; i<2; i++) {
					result.add(threatSpaces.get(first).get(3).get(i).getP());
				}
				if (19 - first.x > first.y) {
					for (int i=0; i<2; i++) {
						result.add(threatSpaces.get(first).get(2).get(i).getP());
					}
				} else {
					for (int i=0; i<2; i++) {
						result.add(threatSpaces.get(first).get(4).get(i).getP());
					}
				}
			} else {
				// quadrant 4
				for (int i=0; i<2; i++) {
					result.add(threatSpaces.get(first).get(5).get(i).getP());
				}
				if (first.x > first.y) {
					for (int i=0; i<2; i++) {
						result.add(threatSpaces.get(first).get(4).get(i).getP());
					}
				} else {
					for (int i=0; i<2; i++) {
						result.add(threatSpaces.get(first).get(6).get(i).getP());
					}
				}
			}
		}
		return result;
	}
}