package step.plugins.timeseries.dashboards.model;

import jakarta.validation.constraints.NotNull;
import step.core.timeseries.metric.MetricAttribute;

import java.util.ArrayList;
import java.util.List;

public class ChartSettings {
	
	@NotNull
	private String metricKey;
	@NotNull
	private List<MetricAttribute> attributes = new ArrayList<>();
	@NotNull
	private AxesSettings primaryAxes;
	
	private AxesSettings secondaryAxes;
	
	@NotNull
	private List<TimeSeriesFilterItem> filters = new ArrayList<>();
	
	private String oql;
	
	@NotNull
	private List<String> grouping = new ArrayList<>();
	@NotNull
	private boolean inheritGlobalFilters;
	@NotNull
	private boolean inheritGlobalGrouping;
	
	@NotNull
	private boolean readonlyGrouping;
	@NotNull
	private boolean readonlyAggregate;

	public List<MetricAttribute> getAttributes() {
		return attributes;
	}

	public ChartSettings setAttributes(List<MetricAttribute> attributes) {
		this.attributes = attributes;
		return this;
	}

	public String getOql() {
		return oql;
	}

	public ChartSettings setOql(String oql) {
		this.oql = oql;
		return this;
	}

	public String getMetricKey() {
		return metricKey;
	}

	public ChartSettings setMetricKey(String metricKey) {
		this.metricKey = metricKey;
		return this;
	}

	public List<TimeSeriesFilterItem> getFilters() {
		return filters;
	}

	public ChartSettings setFilters(List<TimeSeriesFilterItem> filters) {
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

	public AxesSettings getSecondaryAxes() {
		return secondaryAxes;
	}

	public ChartSettings setSecondaryAxes(AxesSettings secondaryAxes) {
		this.secondaryAxes = secondaryAxes;
		return this;
	}
}
