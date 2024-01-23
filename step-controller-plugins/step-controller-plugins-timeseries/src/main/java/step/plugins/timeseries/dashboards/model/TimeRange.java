package step.plugins.timeseries.dashboards.model;

import jakarta.validation.constraints.NotNull;

public class TimeRange {
	@NotNull
	private long from;
	@NotNull
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
