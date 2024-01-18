package step.plugins.timeseries.dashboards.model;

import jakarta.validation.constraints.NotNull;

public class DashboardItem { // T = 'ChartSettings'. class: 'ChartSettings' @JsonTypeInfo
	
	@NotNull
	private String name;
	@NotNull
	private DashletType type;
	private ChartSettings chartSettings;
	@NotNull
	private int size = 1; // full width is 2
	// one setting for each type


	public String getName() {
		return name;
	}

	public DashboardItem setName(String name) {
		this.name = name;
		return this;
	}

	public DashletType getType() {
		return type;
	}

	public DashboardItem setType(DashletType type) {
		this.type = type;
		return this;
	}

	public ChartSettings getChartSettings() {
		return chartSettings;
	}

	public DashboardItem setChartSettings(ChartSettings chartSettings) {
		this.chartSettings = chartSettings;
		return this;
	}

	public int getSize() {
		return size;
	}

	public DashboardItem setSize(int size) {
		this.size = size;
		return this;
	}
}
