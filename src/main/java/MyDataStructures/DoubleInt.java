package MyDataStructures;

/**
 * For performance reporting purposes
 */
public class DoubleInt {
	private double d;
	private int i;
	
	public DoubleInt() {
		d = 0; i = 0;
	}
	
	public void incrementD(double inc) {
		d += inc;
	}
	
	public void incrementI() {
		i++;
	}
	
	public double getAvg() {
		return d / i;
	}
}
