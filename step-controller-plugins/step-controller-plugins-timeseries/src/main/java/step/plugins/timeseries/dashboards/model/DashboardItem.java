package step.plugins.timeseries.dashboards.model;

import jakarta.validation.constraints.NotNull;
import step.core.timeseries.metric.MetricAttribute;

import java.util.ArrayList;
import java.util.List;

public class DashboardItem { // T = 'ChartSettings'. class: 'ChartSettings' @JsonTypeInfo
	
	@NotNull
	private String id;
	@NotNull
	private String name;
	@NotNull
	private DashletType type;
	
	private String masterChartId;
	
	@NotNull
	private String metricKey;
	@NotNull
	private List<MetricAttribute> attributes = new ArrayList<>();
	
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
	
	private ChartSettings chartSettings; // for charts only
	
	private TableSettings tableSettings; // for tables only
	
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

	public String getId() {
		return id;
	}

	public DashboardItem setId(String id) {
		this.id = id;
		return this;
	}

	public String getMasterChartId() {
		return masterChartId;
	}

	public DashboardItem setMasterChartId(String masterChartId) {
		this.masterChartId = masterChartId;
		return this;
	}

	public String getMetricKey() {
		return metricKey;
	}

	public DashboardItem setMetricKey(String metricKey) {
		this.metricKey = metricKey;
		return this;
	}

	public List<MetricAttribute> getAttributes() {
		return attributes;
	}

	public DashboardItem setAttributes(List<MetricAttribute> attributes) {
		this.attributes = attributes;
		return this;
	}

	public List<TimeSeriesFilterItem> getFilters() {
		return filters;
	}

	public DashboardItem setFilters(List<TimeSeriesFilterItem> filters) {
		this.filters = filters;
		return this;
	}

	public String getOql() {
		return oql;
	}

	public DashboardItem setOql(String oql) {
		this.oql = oql;
		return this;
	}

	public List<String> getGrouping() {
		return grouping;
	}

	public DashboardItem setGrouping(List<String> grouping) {
		this.grouping = grouping;
		return this;
	}

	public boolean isInheritGlobalFilters() {
		return inheritGlobalFilters;
	}

	public DashboardItem setInheritGlobalFilters(boolean inheritGlobalFilters) {
		this.inheritGlobalFilters = inheritGlobalFilters;
		return this;
	}

	public boolean isInheritGlobalGrouping() {
		return inheritGlobalGrouping;
	}

	public DashboardItem setInheritGlobalGrouping(boolean inheritGlobalGrouping) {
		this.inheritGlobalGrouping = inheritGlobalGrouping;
		return this;
	}

	public boolean isReadonlyGrouping() {
		return readonlyGrouping;
	}

	public DashboardItem setReadonlyGrouping(boolean readonlyGrouping) {
		this.readonlyGrouping = readonlyGrouping;
		return this;
	}

	public boolean isReadonlyAggregate() {
		return readonlyAggregate;
	}

	public DashboardItem setReadonlyAggregate(boolean readonlyAggregate) {
		this.readonlyAggregate = readonlyAggregate;
		return this;
	}

	public TableSettings getTableSettings() {
		return tableSettings;
	}

	public DashboardItem setTableSettings(TableSettings tableSettings) {
		this.tableSettings = tableSettings;
		return this;
	}
}
