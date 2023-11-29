package step.plugins.timeseries.dashboards.model;

import java.util.List;

public class ChartSettings {
	
	private AxesSettings primaryAxes;
	private AxesSettings secondaryAxes;
	private List<String> grouping;
	private boolean inheritGlobalFilters;
	private boolean inheritGlobalGrouping;
	
	private List<ChartFilterItem> filters;

	public List<String> getGrouping() {
		return grouping;
	}

	public ChartSettings setGrouping(List<String> grouping) {
		this.grouping = grouping;
		return this;
	}

	public AxesSettings getPrimaryAxes() {
		return primaryAxes;
	}

	public ChartSettings setPrimaryAxes(AxesSettings primaryAxes) {
		this.primaryAxes = primaryAxes;
		return this;
	}

	public AxesSettings getSecondaryAxes() {
		return secondaryAxes;
	}

	public ChartSettings setSecondaryAxes(AxesSettings secondaryAxes) {
		this.secondaryAxes = secondaryAxes;
		return this;
	}

	public boolean isInheritGlobalFilters() {
		return inheritGlobalFilters;
	}

	public ChartSettings setInheritGlobalFilters(boolean inheritGlobalFilters) {
		this.inheritGlobalFilters = inheritGlobalFilters;
		return this;
	}

	public boolean isInheritGlobalGrouping() {
		return inheritGlobalGrouping;
	}

	public ChartSettings setInheritGlobalGrouping(boolean inheritGlobalGrouping) {
		this.inheritGlobalGrouping = inheritGlobalGrouping;
		return this;
	}

	public List<ChartFilterItem> getFilters() {
		return filters;
	}

	public ChartSettings setFilters(List<ChartFilterItem> filters) {
		this.filters = filters;
		return this;
	}
}
