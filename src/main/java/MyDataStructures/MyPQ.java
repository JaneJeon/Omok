package MyDataStructures;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.*;
import java.util.List;

// custom priority queue since a. Java's default priority queue is actually a binary heap, and
// b. I want to limit the amount of calculation I have to do because I only care about the top 5
// also, it sorts by absolute value
public class MyPQ {
	private int limit, size;
	private List<Integer> top;
	private List<Point> points;

	public MyPQ(int limit) {
		this.limit = limit;
		top = new IntArrayList(limit);
		points = new ObjectArrayList<>(limit);
		size = 0;
	}

	public void push(int n, Point p) {
		if (size == 0) {
			top.add(n);
			points.add(p);
			size++;
		} else {
			int place = size;
			for (int i = size - 1; i >= 0; i--) {
				if (Math.abs(top.get(i)) > Math.abs(n)) break;
				place = i;
			}
			if (place < size) {
				if (size < limit) {
					top.add(place, n);
					points.add(place, p);
					size++;
				} else {
					top.remove(size - 1);
					points.remove(size - 1);
					top.add(place, n);
					points.add(place, p);
				}
			}
		}
	}

	public Point pop() {
		top.remove(0);
		size--;
		return points.remove(0);
	}

	public int peek() {
		return top.get(0);
	}

	public Point peekP() {
		return points.get(0);
	}

	public String toString() {
		String result = "";
		for (int i=0; i<size; i++) {
			result += "<("+points.get(i).x+", "+points.get(i).y+"), "+top.get(i)+"> ";
		}
		return result;
	}

	public int size() {
		return size;
	}
}
