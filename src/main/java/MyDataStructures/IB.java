package MyDataStructures;

import java.io.Serializable;

/*
 * @author: Sungil Ahn
 */

// custom data type for holding int[][] and boolean
public class IB implements Serializable {
	private static final long serialVersionUID = 135792056789000807L;

	/**
	 * @serial
	 */
	private int[][] array;
	/**
	 * @serial
	 */
	private boolean bool;

	public IB(int[][] array, boolean bool) {
		this.array = array; this.bool = bool;
	}

	public int[][] getArray() {
		return this.array;
	}

	public void setArray(int[][] array) {
		this.array = array;
	}

	public boolean getBool() {
		return this.bool;
	}

	public void setBool(boolean bool) {
		this.bool = bool;
	}
}
