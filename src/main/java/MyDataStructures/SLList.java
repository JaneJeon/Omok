package MyDataStructures;

/*
 * @author: Sungil Ahn
 */

// Singly-LInked List to use as a stack when size < limit and queue when size = limit
public class SLList<T> {
	private final int limit;
	private Element head, tail;
	private int size;

	public SLList(int limit) {
		this.head = null;
		this.tail = null;
		this.size = 0;
		this.limit = limit;
	}

	public void add(T item) {
		if (this.isEmpty()) {
			this.head = new Element(item);
			this.tail = this.head;
		} else {
			if (this.size == this.limit) {
				this.head = this.head.next;
				this.size--;
			}
			this.tail.next = new Element(item);
			this.tail = this.tail.next;
		}
		this.size++;
	}

	// always remove from tail
	public T pop() {
		if (this.size > 1) {
			this.tail = this.advance(this.size - 2);
			T data = this.tail.next.data; // tail.next may not exist when size is 1
			tail.next = null;
			this.size--;
			return data;
		} else {
			T data = this.tail.data;
			this.tail = null;
			this.head = null;
			this.size--;
			return data;
		}
	}

	private boolean isEmpty() {
		return this.head == null;
	}

	// advances index number of times from element 0
	private Element advance(int index) {
		Element result = this.head;
		for (int i=0; i<index; i++) {
			result = result.next;
		}
		return result;
	}

	public int getSize() {
		return this.size;
	}

	public T getHeadData() {
		return this.head.data;
	}

	public T getTailData() {
		return this.tail.data;
	}

	private class Element {
		private final T data;
		private Element next;

		public Element(T data) {
			this.data = data;
			this.next = null;
		}
	}
}
