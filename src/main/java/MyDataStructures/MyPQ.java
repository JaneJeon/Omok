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
// b. I want to limit the amount of calculation I have to do because I only care about the top 5
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
		this.top = new IntArrayList(limit);
		this.points = new ObjectArrayList<>(limit);
		this.debug = debug;
		if (!debug) this.lowestValues = new ObjectArrayList<>(limit);
		this.full = 0;
	}

	public void push(int n, Point p) {
		if (this.top.size() == 0) {
			this.top.add(n);
			this.points.add(p);
		} else {
			int place = this.top.size();
			for (int i = this.top.size() - 1; i >= 0; i--) {
				if (Math.abs(this.top.get(i)) > Math.abs(n)) break;
				place = i;
			}
			if (place < this.top.size()) {
				if (this.top.size() < this.limit) {
					this.top.add(place, n);
					this.points.add(place, p);
				} else {
					this.top.remove(this.top.size() - 1);
					this.points.remove(this.points.size() - 1);
					this.top.add(place, n);
					this.points.add(place, p);
				}
			} else if (this.top.size() < this.limit) {
				this.top.add(n);
				this.points.add(p);
			}
			if (this.top.size() == this.limit) this.full++;
		}
		if (!this.debug && this.top.size() == this.limit) {
			int lastValue = this.top.get(this.limit - 1);
			Point lastPoint = this.points.get(this.limit - 1);
			if (this.lowestValues.isEmpty()) {
				this.lowestValues.add(new PI(lastPoint, lastValue));
			} else {
				// compare the value of the last point in the list with that of the lowestValues
				if (Math.abs(lastValue) == Math.abs(this.lowestValues.get(0).getI())) {
					this.lowestValues.add(new PI(lastPoint, lastValue));
				} else if (this.full == 1 | Math.abs(lastValue) > Math.abs(this.lowestValues.get(0).getI())) {
					// erase the lowest values and fill it with that of higher values
					this.lowestValues.clear();
					this.lowestValues.add(new PI(lastPoint, lastValue));
				}
			}
		}
	}

	public Point pop() {
		if (!this.debug && !this.lowestValues.isEmpty() && (this.top.isEmpty() ||
			Math.abs(this.top.get(0)) == Math.abs(this.lowestValues.get(0).getI()))) {
			PI p = this.lowestValues.get(new Random().nextInt(this.lowestValues.size()));
			this.lowestValues.remove(p);
			return p.getP();
		}
		this.top.remove(0);
		return this.points.remove(0);
	}

	public int peek() {
		return this.top.get(0);
	}
	
	public String toString() {
		String result = "";
		for (int i = 0; i < this.top.size(); i++) {
			result += "<(" + this.points.get(i).x + ", " + this.points.get(i).y + "), " + this.top.get(i) + "> ";
		}
		return result;
	}
	
	public int numLowest() {
		return this.lowestValues.size();
	}
}