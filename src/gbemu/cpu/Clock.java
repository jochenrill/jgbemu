package gbemu.cpu;

public class Clock {

	public int m;
	public int t;

	protected Clock() {
		m = 0;
		t = 0;
	}

	public void reset() {
		m = 0;
		t = 0;
	}

}
