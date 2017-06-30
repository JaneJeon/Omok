package MyDataStructures;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.*;
import java.util.List;
import java.util.Random;

/*
 * @author: Sungil Ahn
 */

// custom priority queue since a. Java's default priority queue is actually a binary heap, and
// b. I want to limit the amount of calculation I have to do because I only care about the top 5.
// also, it sorts by absolute value
public class MyPQ {
	private final int limit;
	private final List<Integer> top;
	private final List<Point> points;
	private final boolean debug;
	private List<PI> lowestValues;
	private int full;

	public MyPQ(int limit, boolean debug) {
		this.limit = limit;
		top = new IntArrayList(limit);
		points = new ObjectArrayList<>(limit);
		this.debug = debug;
		if (!debug) lowestValues = new ObjectArrayList<>(limit);
		full = 0;
	}

	public void push(int n, Point p) {
		if (top.size() == 0) {
			top.add(n);
			points.add(p);
		} else {
			int place = top.size();
			for (int i = top.size() - 1; i >= 0; i--) {
				if (Math.abs(top.get(i)) > Math.abs(n)) break;
				place = i;
			}
			if (place < top.size()) {
				if (top.size() < limit) {
					top.add(place, n);
					points.add(place, p);
				} else {
					top.remove(top.size() - 1);
					points.remove(points.size() - 1);
					top.add(place, n);
					points.add(place, p);
				}
			} else if (top.size() < limit) {
				top.add(n);
				points.add(p);
			}
			if (top.size() == limit) full++;
		}
		if (!debug && top.size() == limit) {
			int lastValue = top.get(limit - 1);
			Point lastPoint = points.get(limit - 1);
			if (lowestValues.isEmpty()) {
				lowestValues.add(new PI(lastPoint, lastValue));
			} else {
				// compare the value of the last point in the list with that of the lowestValues
				if (Math.abs(lastValue) == Math.abs(lowestValues.get(0).getI())) {
					lowestValues.add(new PI(lastPoint, lastValue));
				} else if (full == 1 | Math.abs(lastValue) > Math.abs(lowestValues.get(0).getI())) {
					// erase the lowest values and fill it with that of higher values
					lowestValues.clear();
					lowestValues.add(new PI(lastPoint, lastValue));
				}
			}
		}
	}

	public Point pop() {
		if (!debug && !lowestValues.isEmpty() && (top.isEmpty() ||
			Math.abs(top.get(0)) == Math.abs(lowestValues.get(0).getI()))) {
			PI p = lowestValues.get(new Random().nextInt(lowestValues.size()));
			lowestValues.remove(p);
			return p.getP();
		}
		top.remove(0);
		return points.remove(0);
	}

	public int peek() {
		return top.get(0);
	}
	
	public String toString() {
		String result = "";
		for (int i = 0; i < top.size(); i++) {
			result += "<(" + points.get(i).x + ", " + points.get(i).y + "), " + top.get(i) + "> ";
		}
		return result;
	}
	
	public int numLowest() {
		return lowestValues.size();
	}
}