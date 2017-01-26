package MyDataStructures;

import Dependencies.DeepCopy;

import java.awt.*;
import java.util.List;
import java.util.Map;

// holds history of the game state - even handles deep copying
public class History {
	private SLList<int[][]> boardHistory;
	private SLList<IB> scoresHistory;
	private SLList<Map<Point, List<List<PI>>>> threatsHistory;
	private SLList<Map<Point, List<List<Point>>>> lookupHistory;

	public History(int limit) {
		boardHistory = new SLList<>(limit);
		scoresHistory = new SLList<>(limit);
		threatsHistory = new SLList<>(limit);
		lookupHistory = new SLList<>(limit);
	}

	public void add(int[][] board, IB scores, Map<Point, List<List<PI>>> threats, Map<Point,
		List<List<Point>>> lookup) {
		boardHistory.add((int[][]) DeepCopy.copy(board));
		scoresHistory.add((IB) DeepCopy.copy(scores)); // not serializable?
		threatsHistory.add((Map) DeepCopy.copy(threats)); // not serializable?
		lookupHistory.add((Map) DeepCopy.copy(lookup));
	}

	public ReturningValues pop() {
		return new ReturningValues(boardHistory.pop(), scoresHistory.pop(), threatsHistory.pop(), lookupHistory.pop());
	}

	public int getSize() {
		return boardHistory.getSize();
	}
}
