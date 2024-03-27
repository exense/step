package step.plugins.timeseries.dashboards.model;

import jakarta.validation.constraints.NotNull;
import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.EnricheableObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardView extends AbstractOrganizableObject implements EnricheableObject {
	
	@NotNull
	private String name;
	private String description;
	private Long resolution;
	private Long refreshInterval;
	@NotNull
	private TimeRangeSelection timeRange;
	@NotNull
	private List<String> grouping;
	@NotNull
	private List<TimeSeriesFilterItem> filters;
	@NotNull
	private List<DashboardItem> dashlets;
	
	private Map<String, Object> metadata = new HashMap<>();

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

	public List<TimeSeriesFilterItem> getFilters() {
		return filters;
	}

	public DashboardView setFilters(List<TimeSeriesFilterItem> filters) {
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

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public DashboardView setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
		return this;
	}

	public Long getRefreshInterval() {
		return refreshInterval;
	}

	public DashboardView setRefreshInterval(Long refreshInterval) {
		this.refreshInterval = refreshInterval;
		return this;
	}
}
