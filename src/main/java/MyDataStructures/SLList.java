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
		head = null;
		tail = null;
		size = 0;
		this.limit = limit;
	}

	public void add(T item) {
		if (isEmpty()) {
			head = new Element(item);
			tail = head;
		} else {
			if (size == limit) {
				head = head.next;
				size--;
			}
			tail.next = new Element(item);
			tail = tail.next;
		}
		size++;
	}

	// always remove from tail
	public T pop() {
		if (size > 1) {
			tail = advance(size - 2);
			T data = tail.next.data; // tail.next may not exist when size is 1
			tail.next = null;
			size--;
			return data;
		} else {
			T data = tail.data;
			tail = null;
			head = null;
			size--;
			return data;
		}
	}

	private boolean isEmpty() {
		return head == null;
	}

	// advances index number of times from element 0
	private Element advance(int index) {
		Element result = head;
		for (int i=0; i<index; i++) {
			result = result.next;
		}
		return result;
	}

	public int getSize() {
		return size;
	}

	public T getHeadData() {
		return head.data;
	}

	public T getTailData() {
		return tail.data;
	}

	private class Element {
		private final T data;
		private Element next;

		public Element(T data) {
			this.data = data;
			next = null;
		}
	}
}
