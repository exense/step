package step.plugins.timeseries.dashboards.model;

public class TimeRange {
	private long from;
	private long to;

	public long getFrom() {
		return from;
	}

	public TimeRange setFrom(long from) {
		this.from = from;
		return this;
	}

	public long getTo() {
		return to;
	}

	public TimeRange setTo(long to) {
		this.to = to;
		return this;
	}
}
