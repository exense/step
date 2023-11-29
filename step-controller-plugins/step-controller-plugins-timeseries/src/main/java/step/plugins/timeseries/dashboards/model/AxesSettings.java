package step.plugins.timeseries.dashboards.model;

import step.core.timeseries.metric.MetricAggregation;

import java.util.List;

public class AxesSettings {

	private String metricKey;
	private MetricAggregation aggregation;
	private AxesDisplayType displayType;
	private String unit;

	public String getMetricKey() {
		return metricKey;
	}

	public AxesSettings setMetricKey(String metricKey) {
		this.metricKey = metricKey;
		return this;
	}

	public MetricAggregation getAggregation() {
		return aggregation;
	}

	public AxesSettings setAggregation(MetricAggregation aggregation) {
		this.aggregation = aggregation;
		return this;
	}

	public AxesDisplayType getDisplayType() {
		return displayType;
	}

	public AxesSettings setDisplayType(AxesDisplayType displayType) {
		this.displayType = displayType;
		return this;
	}

	public String getUnit() {
		return unit;
	}

	public AxesSettings setUnit(String unit) {
		this.unit = unit;
		return this;
	}
}
