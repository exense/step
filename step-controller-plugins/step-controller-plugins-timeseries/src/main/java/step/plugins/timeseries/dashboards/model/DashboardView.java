package step.plugins.timeseries.dashboards.model;

import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.EnricheableObject;

import java.util.List;

public class DashboardView extends AbstractOrganizableObject implements EnricheableObject {
	
	private String name;
	private Long resolution;
	private TimeRangeSelection timeRange;
	private List<String> grouping;
	private List<ChartFilterItem> filters;
	private List<DashboardItem> dashlets;

	public String getName() {
		return name;
	}

	public DashboardView setName(String name) {
		this.name = name;
		return this;
	}

	public Long getResolution() {
		return resolution;
	}

	public DashboardView setResolution(Long resolution) {
		this.resolution = resolution;
		return this;
	}

	public TimeRangeSelection getTimeRange() {
		return timeRange;
	}

	public DashboardView setTimeRange(TimeRangeSelection timeRange) {
		this.timeRange = timeRange;
		return this;
	}

	public List<String> getGrouping() {
		return grouping;
	}

	public DashboardView setGrouping(List<String> grouping) {
		this.grouping = grouping;
		return this;
	}

	public List<ChartFilterItem> getFilters() {
		return filters;
	}

	public DashboardView setFilters(List<ChartFilterItem> filters) {
		this.filters = filters;
		return this;
	}

	public List<DashboardItem> getDashlets() {
		return dashlets;
	}

	public DashboardView setDashlets(List<DashboardItem> dashlets) {
		this.dashlets = dashlets;
		return this;
	}
}
