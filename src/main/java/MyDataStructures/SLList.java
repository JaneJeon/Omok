package MyDataStructures;

// Singly-LInked List to use as a stack when size < limit and queue when size = limit
public class SLList<T> {
	private Element head, tail;
	private int size;
	private final int limit;

	private class Element {
		private T data;
		private Element next;

		public Element(T data) {
			this.data = data;
			this.next = null;
		}
	}

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

	/* tests - passed as of version 0.5.3
	public static void main(String[] args) {
		MyDataStructures.SLList foo = new MyDataStructures.SLList(6);
		foo.add(1);
		foo.add(2);
		foo.add(3);
		foo.add(4);
		foo.add(5);
		System.out.println("size: "+foo.size+", head: "+foo.head.data+", tail: "+foo.tail.data);
		foo.add(6);
		System.out.println("pop: "+foo.pop());
		foo.add(66);
		foo.add(77);
		foo.add(88);
		foo.add(99);
		System.out.println("size: "+foo.size+", head: "+foo.head.data+", tail: "+foo.tail.data);
		System.out.println("pop: "+foo.pop());
		System.out.println("size: "+foo.size+", head: "+foo.head.data+", tail: "+foo.tail.data);
	}
	*/
}
