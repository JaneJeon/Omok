package MyDataStructures;

import java.awt.*;
import java.util.List;
import java.util.Map;

// struct-like class to store return values for MyDataStructures.History's undo method
public class ReturningValues {
	public final int[][] board;
	public final IB scores;
	public final Map<Point, List<List<PI>>> threats;
	public final Map<Point, List<List<Point>>> lookup;

	public ReturningValues(int[][] board, IB scores, Map<Point, List<List<PI>>> threats, Map<Point,
		List<List<Point>>> lookup) {
		this.board = board;
		this.scores = scores;
		this.threats = threats;
		this.lookup = lookup;
	}
}
