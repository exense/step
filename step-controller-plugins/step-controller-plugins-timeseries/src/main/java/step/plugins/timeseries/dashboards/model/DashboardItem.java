package step.plugins.timeseries.dashboards.model;

public class DashboardItem {
	
	private String name;
	private DashletType type;
	private ChartSettings chartSettings;
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
}
