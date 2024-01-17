package step.plugins.timeseries;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.timeseries.metric.MetricAggregation;
import step.core.timeseries.metric.MetricAttribute;
import step.core.timeseries.metric.MetricRenderingSettings;
import step.plugins.timeseries.dashboards.DashboardAccessor;
import step.plugins.timeseries.dashboards.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static step.plugins.timeseries.TimeSeriesExecutionPlugin.EXECUTIONS_COUNT;
import static step.plugins.timeseries.TimeSeriesExecutionPlugin.RESPONSE_TIME;

public class DashboardTest {
	
	private static Collection<DashboardView> dashboardViewCollection = new InMemoryCollection<>();
	private DashboardAccessor dashboardAccessor = new DashboardAccessor(dashboardViewCollection);
	
	@BeforeClass
    public static void init() {
		dashboardViewCollection.remove(Filters.empty());
    }
	
	@Test
	public void baseTest() {
		DashboardView testDashboard = createTestDashboard();
		testDashboard = dashboardAccessor.save(testDashboard);
		dashboardAccessor.save(testDashboard);
		List<DashboardView> foundDashboards = dashboardAccessor.findByIds(List.of(testDashboard.getId().toString())).collect(Collectors.toList());
		Assert.assertEquals(1, foundDashboards.size());
		DashboardView foundDashboard = foundDashboards.get(0);
		Assert.assertEquals(foundDashboard.getName(), testDashboard.getName());
		Assert.assertEquals(foundDashboard.getDescription(), testDashboard.getDescription());
		Assert.assertEquals(foundDashboard.getTimeRange(), testDashboard.getTimeRange());
		Assert.assertEquals(foundDashboard.getResolution(), testDashboard.getResolution());
		Assert.assertEquals(foundDashboard.getDashlets().size(), testDashboard.getDashlets().size());
		Assert.assertEquals(foundDashboard.getFilters().size(), testDashboard.getFilters().size());
		Assert.assertEquals(foundDashboard.getMetadata().size(), testDashboard.getMetadata().size());
		Assert.assertEquals(foundDashboard.getTimeRange().getType(), testDashboard.getTimeRange().getType());
		assertTimeRangeEquals(foundDashboard.getTimeRange().getAbsoluteSelection(), testDashboard.getTimeRange().getAbsoluteSelection());
	}
	
	@Test
	public void findLegacyDashboardTest() {
		DashboardView legacyDashboard = new DashboardView()
				.setName("Legacy")
				.setMetadata(Map.of("isLegacy", true));
		DashboardView normalDashboard = createTestDashboard();
		legacyDashboard = dashboardAccessor.save(legacyDashboard);
		normalDashboard = dashboardAccessor.save(normalDashboard);
		List<DashboardView> foundDashboards = dashboardAccessor.findLegacyDashboards().collect(Collectors.toList());
		Assert.assertEquals(1, foundDashboards.size());
		Assert.assertEquals(legacyDashboard.getId(), foundDashboards.get(0).getId());
		Assert.assertEquals(legacyDashboard.getName(), foundDashboards.get(0).getName());
	}
	
	private void assertTimeRangeEquals(TimeRange t1, TimeRange t2) {
		if (t1 == null && t2 == null) {
			return;
		}
		Assert.assertEquals(t1.getFrom(), t2.getFrom());
		Assert.assertEquals(t1.getTo(), t2.getTo());
	}
	
	private DashboardView createTestDashboard() {
		MetricAttribute taskAttribute = new MetricAttribute().setName("taskId").setDisplayName("Task");
		MetricAttribute executionAttribute = new MetricAttribute().setName("eId").setDisplayName("Execution");
		MetricAttribute planAttribute = new MetricAttribute().setName("planId").setDisplayName("Plan");
		MetricAttribute nameAttribute = new MetricAttribute().setName("name").setDisplayName("Name");

		return new DashboardView()
				.setName("Initial Dashboard")
				.setDescription("This is a generated dashboard, for development")
				.setTimeRange(new TimeRangeSelection()
						.setType(TimeRangeSelectionType.ABSOLUTE)
						.setAbsoluteSelection(new TimeRange().setFrom(1700152446408L).setTo(1700155195285L))
				)
				.setFilters(Arrays.asList(
						new TimeSeriesFilterItem()
								.setLabel("Status")
								.setAttribute("rnStatus")
								.setTextOptions(Arrays.asList("PASSED", "FAILED", "TECHNICAL_ERROR", "INTERRUPTED"))
								.setTextValues(Arrays.asList("PASSED"))
								.setType(TimeSeriesFilterItemType.OPTIONS)
								.setExactMatch(true),
						new TimeSeriesFilterItem()
								.setLabel("Type")
								.setTextOptions(Arrays.asList("keyword", "custom"))
								.setType(TimeSeriesFilterItemType.OPTIONS)
								.setAttribute("type")
								.setExactMatch(true),
						new TimeSeriesFilterItem()
								.setLabel("Name")
								.setType(TimeSeriesFilterItemType.FREE_TEXT)
								.setAttribute("name"),
						new TimeSeriesFilterItem()
								.setLabel("Execution")
								.setAttribute("eId")
								.setType(TimeSeriesFilterItemType.EXECUTION),
						new TimeSeriesFilterItem()
								.setLabel("Origin")
								.setType(TimeSeriesFilterItemType.FREE_TEXT)
								.setAttribute("origin"),
						new TimeSeriesFilterItem()
								.setLabel("Task")
								.setAttribute("taskId")
								.setType(TimeSeriesFilterItemType.TASK),
						new TimeSeriesFilterItem()
								.setLabel("Plan")
								.setAttribute("planId")
								.setType(TimeSeriesFilterItemType.PLAN)
				))
				.setDashlets(Arrays.asList(
						new DashboardItem()
								.setName("Response times dashlet")
								.setType(DashletType.CHART)
								.setChartSettings(new ChartSettings()
										.setAttributes(Arrays.asList(nameAttribute, taskAttribute, executionAttribute, planAttribute))
										.setMetricKey("response-time")
										.setInheritGlobalFilters(true)
										.setGrouping(Arrays.asList("name"))
										.setPrimaryAxes(new AxesSettings()
												.setAggregation(MetricAggregation.AVG)
												.setDisplayType(AxesDisplayType.LINE)
												.setUnit("ms")
										)
								),
						new DashboardItem()
								.setName("Executions count")
								.setType(DashletType.CHART)
								.setChartSettings(new ChartSettings()
										.setMetricKey(EXECUTIONS_COUNT)
										.setInheritGlobalFilters(false)
										.setGrouping(List.of())
										.setAttributes(Arrays.asList(taskAttribute, executionAttribute, planAttribute))
										.setPrimaryAxes(new AxesSettings()
												.setAggregation(MetricAggregation.SUM)
												.setDisplayType(AxesDisplayType.LINE)
												.setUnit("1")
										)
								),
						new DashboardItem()
								.setName("Statuses")
								.setType(DashletType.CHART)
								.setChartSettings(new ChartSettings()
										.setMetricKey(RESPONSE_TIME)
										.setInheritGlobalFilters(false)
										.setGrouping(List.of("rnStatus"))
										.setReadonlyGrouping(true)
										.setAttributes(Arrays.asList(nameAttribute, taskAttribute, executionAttribute, planAttribute))
										.setPrimaryAxes(new AxesSettings()
												.setAggregation(MetricAggregation.COUNT)
												.setDisplayType(AxesDisplayType.LINE)
												.setUnit("1")
												.setRenderingSettings(new MetricRenderingSettings()
														.setSeriesColors(Map.of("FAILED", "#d9534f",
																"PASSED", "#5cb85c",
																"INTERRUPTED", "#f9c038",
																"TECHNICAL_ERROR", "#000000"))
												)
										)
								)
				));
		
	}

}
