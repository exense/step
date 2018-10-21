package step.grid.client;

public class GridClientConfiguration {

	private long noMatchExistsTimeout = 10000;
	
	private long matchExistsTimeout = 60000;
	
	private int releaseSessionTimeout = 60000;
	
	private int reserveSessionTimeout = 10000;
	
	private int readTimeoutOffset = 3000;

	public long getNoMatchExistsTimeout() {
		return noMatchExistsTimeout;
	}

	public void setNoMatchExistsTimeout(long noMatchExistsTimeout) {
		this.noMatchExistsTimeout = noMatchExistsTimeout;
	}

	public long getMatchExistsTimeout() {
		return matchExistsTimeout;
	}

	public void setMatchExistsTimeout(long matchExistsTimeout) {
		this.matchExistsTimeout = matchExistsTimeout;
	}

	public int getReleaseSessionTimeout() {
		return releaseSessionTimeout;
	}

	public void setReleaseSessionTimeout(int releaseSessionTimeout) {
		this.releaseSessionTimeout = releaseSessionTimeout;
	}

	public int getReserveSessionTimeout() {
		return reserveSessionTimeout;
	}

	public void setReserveSessionTimeout(int reserveSessionTimeout) {
		this.reserveSessionTimeout = reserveSessionTimeout;
	}

	public int getReadTimeoutOffset() {
		return readTimeoutOffset;
	}

	public void setReadTimeoutOffset(int readTimeoutOffset) {
		this.readTimeoutOffset = readTimeoutOffset;
	}
}
