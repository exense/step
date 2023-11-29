package step.plugins.timeseries.dashboards.model;

import javax.annotation.Nullable;

public class TimeRangeSelection {
	private TimeRangeSelectionType type;
	private TimeRange absoluteSelection;
	private Long relativeRangeMs;

	public TimeRangeSelectionType getType() {
		return type;
	}

	public TimeRangeSelection setType(TimeRangeSelectionType type) {
		this.type = type;
		return this;
	}

	public TimeRange getAbsoluteSelection() {
		return absoluteSelection;
	}

	public TimeRangeSelection setAbsoluteSelection(TimeRange absoluteSelection) {
		this.absoluteSelection = absoluteSelection;
		return this;
	}

	public Long getRelativeRangeMs() {
		return relativeRangeMs;
	}

	public TimeRangeSelection setRelativeRangeMs(Long relativeRangeMs) {
		this.relativeRangeMs = relativeRangeMs;
		return this;
	}
}
