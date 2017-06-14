package MyDataStructures;

import Dependencies.DeepCopy;

import java.awt.*;
import java.util.List;
import java.util.Map;

/*
 * @author: Sungil Ahn
 */

// holds history of the game state - even handles deep copying
public class History {
	private final SLList<int[][]> boardHistory;
	private final SLList<IB> scoresHistory;
	private final SLList<Map<Point, List<List<PI>>>> threatsHistory;
	private final SLList<Map<Point, List<List<Point>>>> lookupHistory;

	public History(int limit) {
		this.boardHistory = new SLList<>(limit);
		this.scoresHistory = new SLList<>(limit);
		this.threatsHistory = new SLList<>(limit);
		this.lookupHistory = new SLList<>(limit);
	}

	public void add(int[][] board, IB scores, Map<Point, List<List<PI>>> threats, Map<Point,
		List<List<Point>>> lookup) {
		this.boardHistory.add((int[][]) DeepCopy.copy(board));
		this.scoresHistory.add((IB) DeepCopy.copy(scores));
		this.threatsHistory.add((Map) DeepCopy.copy(threats));
		this.lookupHistory.add((Map) DeepCopy.copy(lookup));
	}

	public Object[] pop() {
		return new Object[]{this.boardHistory.pop(), this.scoresHistory.pop(), this.threatsHistory.pop(), this.lookupHistory.pop()};
	}

	public int getSize() {
		return this.boardHistory.getSize();
	}
}