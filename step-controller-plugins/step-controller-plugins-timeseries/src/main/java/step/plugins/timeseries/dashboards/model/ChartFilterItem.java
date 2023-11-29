package step.plugins.timeseries.dashboards.model;

import java.util.List;

public class ChartFilterItem {
	
	private String attribute;
	private ChartFilterItemType type;
	
	private Long min;
	private Long max;
	private List<String> textValues;
	private boolean exactMatch;

	public String getAttribute() {
		return attribute;
	}

	public ChartFilterItem setAttribute(String attribute) {
		this.attribute = attribute;
		return this;
	}

	public ChartFilterItemType getType() {
		return type;
	}

	public ChartFilterItem setType(ChartFilterItemType type) {
		this.type = type;
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
