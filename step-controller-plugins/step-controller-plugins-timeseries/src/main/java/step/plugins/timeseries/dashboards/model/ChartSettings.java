package step.plugins.timeseries.dashboards.model;

import step.core.timeseries.metric.MetricAttribute;

import java.util.List;

public class ChartSettings {
	
	private String metricKey;
	private List<MetricAttribute> attributes;
	private AxesSettings primaryAxes;
	private List<ChartFilterItem> filters;
	private List<String> grouping;
	private boolean inheritGlobalFilters;
	private boolean inheritGlobalGrouping;
	
	private boolean readonlyGrouping;
	private boolean readonlyAggregate;

	public List<MetricAttribute> getAttributes() {
		return attributes;
	}

	public ChartSettings setAttributes(List<MetricAttribute> attributes) {
		this.attributes = attributes;
		return this;
	}

	public String getMetricKey() {
		return metricKey;
	}

	public ChartSettings setMetricKey(String metricKey) {
		this.metricKey = metricKey;
		return this;
	}

	public List<ChartFilterItem> getFilters() {
		return filters;
	}

	public ChartSettings setFilters(List<ChartFilterItem> filters) {
		this.filters = filters;
		return this;
	}

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

	public boolean isReadonlyGrouping() {
		return readonlyGrouping;
	}

	public ChartSettings setReadonlyGrouping(boolean readonlyGrouping) {
		this.readonlyGrouping = readonlyGrouping;
		return this;
	}

	public boolean isReadonlyAggregate() {
		return readonlyAggregate;
	}

	public ChartSettings setReadonlyAggregate(boolean readonlyAggregate) {
		this.readonlyAggregate = readonlyAggregate;
		return this;
	}
}
