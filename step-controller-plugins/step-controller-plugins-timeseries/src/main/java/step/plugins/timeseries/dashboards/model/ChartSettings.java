package step.plugins.timeseries.dashboards.model;

import jakarta.validation.constraints.NotNull;
import step.core.timeseries.metric.MetricAttribute;

import java.util.ArrayList;
import java.util.List;

public class ChartSettings {
	
	
	@NotNull
	private AxesSettings primaryAxes;
	
	private AxesSettings secondaryAxes;
	

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
}
