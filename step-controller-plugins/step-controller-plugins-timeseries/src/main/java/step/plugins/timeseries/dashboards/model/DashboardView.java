package step.plugins.timeseries.dashboards.model;

import jakarta.validation.constraints.NotNull;
import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.EnricheableObject;

import java.util.List;

public class DashboardView extends AbstractOrganizableObject implements EnricheableObject {
	
	@NotNull
	private String name;
	private String description;
	private Long resolution;
	@NotNull
	private TimeRangeSelection timeRange;
	@NotNull
	private List<String> grouping;
	@NotNull
	private List<ChartFilterItem> filters;
	@NotNull
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

	public String getDescription() {
		return description;
	}

	public DashboardView setDescription(String description) {
		this.description = description;
		return this;
	}
}
