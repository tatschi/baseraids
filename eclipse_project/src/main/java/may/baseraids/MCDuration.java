package may.baseraids;

public class MCDuration {

	private long ticks;

	public MCDuration(long ticks) {
		setTicks(ticks);
	}
	
	public MCDuration() {}
	
	public long getTicks() {
		return ticks;
	}

	public MCDuration setTicks(long ticks) {
		this.ticks = ticks;
		return this;
	}

	public int getSec() {
		return ticksToSec(ticks);
	}

	public MCDuration setSec(int sec) {
		setTicks(secToTicks(sec));
		return this;
	}

	public int getMin() {
		return secToMin(getSec());
	}

	public MCDuration setMin(int min) {
		setSec(minToSec(min));
		return this;
	}
	
	public MCDuration addTicks(long ticks) {
		this.ticks += ticks;
		return this;
	}
	
	public MCDuration addSec(int sec) {
		addTicks(secToTicks(sec));
		return this;
	}
	
	public MCDuration addMin(int min) {
		addSec(minToSec(min));
		return this;
	}
	
	public MCDuration addDuration(MCDuration duration) {
		this.ticks += duration.getTicks();
		return this;
	}
	
	public MCDuration subtractTicks(long ticks) {
		this.ticks -= ticks;
		return this;
	}
	
	public MCDuration subtractSec(int sec) {
		subtractTicks(secToTicks(sec));
		return this;
	}
	
	public MCDuration subtractMin(int min) {
		subtractSec(minToSec(min));
		return this;
	}
	
	public MCDuration subtractDuration(MCDuration duration) {
		this.ticks -= duration.getTicks();
		return this;
	}
	
	private long secToTicks(int sec) {
		return (long) sec * 20;
	}
	
	private int ticksToSec(long ticks) {
		return (int) (ticks / 20);
	}
		
	private int minToSec(int min) {
		return min * 60;
	}

	private int secToMin(int sec) {
		return sec / 60;
	}
	
	/**
	 * Converts the duration into a string that can be used for displaying purposes.
	 * 
	 * @return a formatted String showing the time until the next raid
	 */
	public String getDisplayString() {
		String displayTime = "";
		if (getMin() > 0) {
			displayTime += getMin() + "min";
		}
		int displayTimeSec = getSec() % 60;
		if (displayTimeSec > 0) {
			displayTime += displayTimeSec + "s";
		}
		return displayTime;
	}
}
