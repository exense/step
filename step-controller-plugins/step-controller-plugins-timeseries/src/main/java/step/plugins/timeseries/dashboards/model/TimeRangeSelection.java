package step.plugins.timeseries.dashboards.model;

import jakarta.validation.constraints.NotNull;

public class TimeRangeSelection {
	@NotNull
	private TimeRangeSelectionType type;
	private TimeRange absoluteSelection;
	private TimeRangeRelativeSelection relativeSelection;

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

	public TimeRangeRelativeSelection getRelativeSelection() {
		return relativeSelection;
	}

	public TimeRangeSelection setRelativeSelection(TimeRangeRelativeSelection relativeSelection) {
		this.relativeSelection = relativeSelection;
		return this;
	}
}
