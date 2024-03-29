package step.plugins.timeseries.dashboards.model;

import jakarta.validation.constraints.NotNull;
import step.core.timeseries.metric.MetricAggregation;
import step.core.timeseries.metric.MetricRenderingSettings;

import java.util.List;

public class AxesSettings {

	@NotNull
	private MetricAggregation aggregation;
	@NotNull
	private AxesDisplayType displayType;
	@NotNull
	private String unit;
	private MetricRenderingSettings renderingSettings;

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


	public MetricRenderingSettings getRenderingSettings() {
		return renderingSettings;
	}

	public AxesSettings setRenderingSettings(MetricRenderingSettings renderingSettings) {
		this.renderingSettings = renderingSettings;
		return this;
	}
}
