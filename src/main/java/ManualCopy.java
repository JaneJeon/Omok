import MyDataStructures.PI;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/*
 * @author: Sungil Ahn
 */

/*
 * A class that handles manual deep copying of data structures, since Java's "copying" methods are all shallow copy,
 * and making changes to a "copy" of the data structure *will* screw up the original one.
 * In addition, while DeepCopy class exists in dependencies folder, it uses serialization, which is painfully slow
 * when you're creating hundreds of thousands of these objects 
 * (however, it is good enough for copying History, which is too complicated for me to copy by hand)
 */
public class ManualCopy {
	public static Map<Integer, String> copyVisits(Map<Integer, String> original) {
		Map<Integer, String> result = new HashMap<>();
		for (Entry<Integer, String> entry : original.entrySet()) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public static Map<Point, List<List<Point>>> copyLookup(Map<Point, List<List<Point>>> original) {
		Map<Point, List<List<Point>>> result = new HashMap<>();
		for (Point threatSpace : original.keySet()) {
			List<List<Point>> branchCopy = new ObjectArrayList<>();
			for (List<Point> threats : original.get(threatSpace)) {
				List<Point> threatsCopy = new ObjectArrayList<>();
				for (Point threat : threats) {
					threatsCopy.add(new Point(threat.x, threat.y));
				}
				branchCopy.add(threatsCopy);
			}
			result.put(new Point(threatSpace.x, threatSpace.y), branchCopy);
		}
		return result;
	}

	public static List<PI> copyList(List<PI> toCopy) {
		List<PI> result = new ObjectArrayList<>();
		for (int i = 0; i < toCopy.size(); i++) {
			result.add(i, new PI(new Point(toCopy.get(i).getP().x, toCopy.get(i).getP().y), toCopy.get(i).getI()));
		}
		return result;
	}

	// deep copies a board while adding a point to it at the same time
	public static int[][] addBoard(int[][] board, int x, int y, int turn) {
		int[][] result = new int[19][19];
		for (int i = 0; i < 19; i++) {
			for (int j = 0; j < 19; j++) {
				result[i][j] = board[i][j];
			}
		}
		result[x][y] = turn;
		return result;
	}
}
