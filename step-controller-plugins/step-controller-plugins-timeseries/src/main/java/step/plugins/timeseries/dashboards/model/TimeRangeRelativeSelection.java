package step.plugins.timeseries.dashboards.model;

import jakarta.validation.constraints.NotNull;

public class TimeRangeRelativeSelection {
	private String label;
	@NotNull
	private Long timeInMs;

	public String getLabel() {
		return label;
	}

	public TimeRangeRelativeSelection setLabel(String label) {
		this.label = label;
		return this;
	}

	public Long getTimeInMs() {
		return timeInMs;
	}

	public TimeRangeRelativeSelection setTimeInMs(Long timeInMs) {
		this.timeInMs = timeInMs;
		return this;
	}
}
