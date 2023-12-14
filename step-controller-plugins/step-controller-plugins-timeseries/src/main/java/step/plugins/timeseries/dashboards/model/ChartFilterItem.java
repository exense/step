package step.plugins.timeseries.dashboards.model;

import java.util.List;

public class ChartFilterItem {
	
	private String label; // optional. should be used for filter options
	private String attribute;
	private ChartFilterItemType type;
	private boolean exactMatch; // for text values
	private boolean isRemovable = true;
	
	private Long min;
	private Long max;
	private List<String> textValues;
	private List<String> textOptions; // for checkbox selection

	public String getLabel() {
		return label;
	}

	public ChartFilterItem setLabel(String label) {
		this.label = label;
		return this;
	}

	public String getAttribute() {
		return attribute;
	}

	public ChartFilterItem setAttribute(String attribute) {
		this.attribute = attribute;
		return this;
	}

	public boolean isRemovable() {
		return isRemovable;
	}

	public ChartFilterItem setRemovable(boolean removable) {
		isRemovable = removable;
		return this;
	}

	public ChartFilterItemType getType() {
		return type;
	}

	public ChartFilterItem setType(ChartFilterItemType type) {
		this.type = type;
		return this;
	}

	public List<String> getTextOptions() {
		return textOptions;
	}

	public ChartFilterItem setTextOptions(List<String> textOptions) {
		this.textOptions = textOptions;
		return this;
	}

	public Long getMin() {
		return min;
	}

	public ChartFilterItem setMin(Long min) {
		this.min = min;
		return this;
	}

	public Long getMax() {
		return max;
	}

	public ChartFilterItem setMax(Long max) {
		this.max = max;
		return this;
	}

	public List<String> getTextValues() {
		return textValues;
	}

	public ChartFilterItem setTextValues(List<String> textValues) {
		this.textValues = textValues;
		return this;
	}

	public boolean isExactMatch() {
		return exactMatch;
	}

	public ChartFilterItem setExactMatch(boolean exactMatch) {
		this.exactMatch = exactMatch;
		return this;
	}
}
