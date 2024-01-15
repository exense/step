package step.plugins.timeseries.dashboards.model;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class TimeSeriesFilterItem {
	
	private String label; // optional. should be used for filter options
	@NotNull
	private String attribute;
	@NotNull
	private TimeSeriesFilterItemType type;
	@NotNull
	private boolean exactMatch; // for text values
	@NotNull
	private boolean isRemovable = true;
	
	private Long min;
	private Long max;
	private List<String> textValues;
	private List<String> textOptions; // for checkbox selection

	public String getLabel() {
		return label;
	}

	public TimeSeriesFilterItem setLabel(String label) {
		this.label = label;
		return this;
	}

	public String getAttribute() {
		return attribute;
	}

	public TimeSeriesFilterItem setAttribute(String attribute) {
		this.attribute = attribute;
		return this;
	}

	public boolean isRemovable() {
		return isRemovable;
	}

	public TimeSeriesFilterItem setRemovable(boolean removable) {
		isRemovable = removable;
		return this;
	}

	public TimeSeriesFilterItemType getType() {
		return type;
	}

	public TimeSeriesFilterItem setType(TimeSeriesFilterItemType type) {
		this.type = type;
		return this;
	}

	public List<String> getTextOptions() {
		return textOptions;
	}

	public TimeSeriesFilterItem setTextOptions(List<String> textOptions) {
		this.textOptions = textOptions;
		return this;
	}

	public Long getMin() {
		return min;
	}

	public TimeSeriesFilterItem setMin(Long min) {
		this.min = min;
		return this;
	}

	public Long getMax() {
		return max;
	}

	public TimeSeriesFilterItem setMax(Long max) {
		this.max = max;
		return this;
	}

	public List<String> getTextValues() {
		return textValues;
	}

	public TimeSeriesFilterItem setTextValues(List<String> textValues) {
		this.textValues = textValues;
		return this;
	}

	public boolean isExactMatch() {
		return exactMatch;
	}

	public TimeSeriesFilterItem setExactMatch(boolean exactMatch) {
		this.exactMatch = exactMatch;
		return this;
	}
}
