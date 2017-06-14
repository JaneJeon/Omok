package Dependencies;

import java.io.InputStream;

// ORIGINAL AUTHOR: http://javatechniques.com/blog/faster-deep-copies-of-java-objects/
/**
 * ByteArrayInputStream implementation that does not synchronize methods.
 */
public class FastByteArrayInputStream extends InputStream {
	/**
	 * Our byte buffer
	 */
	protected byte[] buf;

	/**
	 * Number of bytes that we can read from the buffer
	 */
	protected int count;

	/**
	 * Number of bytes that have been read from the buffer
	 */
	protected int pos;

	public FastByteArrayInputStream(byte[] buf, int count) {
		this.buf = buf;
		this.count = count;
	}

	public final int available() {
		return this.count - this.pos;
	}

	public final int read() {
		return (this.pos < this.count) ? (this.buf[this.pos++] & 0xff) : -1;
	}

	public final int read(byte[] b, int off, int len) {
		if (this.pos >= this.count)
			return -1;

		if ((this.pos + len) > this.count)
			len = (this.count - this.pos);

		System.arraycopy(this.buf, this.pos, b, off, len);
		this.pos += len;
		return len;
	}

	public final long skip(long n) {
		if ((this.pos + n) > this.count)
			n = this.count - this.pos;
		if (n < 0)
			return 0;
		this.pos += n;
		return n;
	}

}