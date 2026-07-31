package step.plugins.timeseries;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.DocumentObject;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.timeseries.bucket.Aggregation;
import step.core.timeseries.metric.MetricAggregation;
import step.core.timeseries.metric.MetricAggregationType;
import step.core.timeseries.metric.MetricAttribute;
import step.core.timeseries.metric.MetricRenderingSettings;
import step.core.timeseries.metric.TwoStageAggregation;
import step.plugins.timeseries.dashboards.DashboardAccessor;
import step.plugins.timeseries.dashboards.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static step.core.metrics.MetricsControllerPlugin.EXECUTIONS_COUNT;
import static step.core.metrics.MetricsControllerPlugin.RESPONSE_TIME;

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
        Assert.assertEquals(foundDashboard.getAttribute(AbstractOrganizableObject.NAME), testDashboard.getAttribute(AbstractOrganizableObject.NAME));
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
        DashboardView legacyDashboard = new DashboardView();
        legacyDashboard.addAttribute(AbstractOrganizableObject.NAME, "Legacy");
        legacyDashboard.setMetadata(Map.of("isLegacy", true));
        DashboardView normalDashboard = createTestDashboard();
        legacyDashboard = dashboardAccessor.save(legacyDashboard);
        normalDashboard = dashboardAccessor.save(normalDashboard);
        List<DashboardView> foundDashboards = dashboardAccessor.findLegacyDashboards().collect(Collectors.toList());
        Assert.assertEquals(1, foundDashboards.size());
        Assert.assertEquals(legacyDashboard.getId(), foundDashboards.get(0).getId());
        Assert.assertEquals(legacyDashboard.getAttribute(AbstractOrganizableObject.NAME), foundDashboards.get(0).getAttribute(AbstractOrganizableObject.NAME));
    }

    /**
     * An aggregation of type TWO_STAGE carries its two stages in its twoStageAggregation.
     */
    @Test
    public void twoStageAggregationRoundTripTest() {
        DashboardView dashboard = new DashboardView().setDashlets(List.of(
            new DashboardItem()
                .setName("Two-stage chart")
                .setType(DashletType.CHART)
                .setMetricKey(RESPONSE_TIME)
                .setAttributes(List.of())
                .setGrouping(List.of())
                .setChartSettings(new ChartSettings()
                    .setPrimaryAxes(new AxesSettings()
                        .setAggregation(twoStageAggregation(Aggregation.AVG, Aggregation.SUM))
                        .setDisplayType(AxesDisplayType.LINE)
                        .setUnit("ms"))),
            new DashboardItem()
                .setName("Two-stage table")
                .setType(DashletType.TABLE)
                .setMetricKey(RESPONSE_TIME)
                .setAttributes(List.of())
                .setGrouping(List.of())
                .setTableSettings(new TableDashletSettings()
                    .setColumns(List.of())
                    .setAggregation(twoStageAggregation(Aggregation.MIN, Aggregation.MAX)))
        ));
        dashboard.addAttribute(AbstractOrganizableObject.NAME, "Two-stage dashboard");

        DashboardView found = saveAndReload(dashboard);

        MetricAggregation primaryAggregation = found.getDashlets().get(0).getChartSettings().getPrimaryAxes().getAggregation();
        Assert.assertEquals(MetricAggregationType.TWO_STAGE, primaryAggregation.getType());
        Assert.assertEquals(new TwoStageAggregation(Aggregation.AVG, Aggregation.SUM), primaryAggregation.getTwoStageAggregation());

        MetricAggregation tableAggregation = found.getDashlets().get(1).getTableSettings().getAggregation();
        Assert.assertEquals(MetricAggregationType.TWO_STAGE, tableAggregation.getType());
        Assert.assertEquals(new TwoStageAggregation(Aggregation.MIN, Aggregation.MAX), tableAggregation.getTwoStageAggregation());
    }

    private MetricAggregation twoStageAggregation(Aggregation timeAggregation, Aggregation groupAggregation) {
        return new MetricAggregation(timeAggregation, groupAggregation);
    }

    /**
     * Dashboards persisted before the two-stage aggregation existed must keep being readable: their aggregation simply
     * carries no twoStageAggregation. This is the backward compatibility requirement, verified through a real Jackson
     * round trip.
     */
    @Test
    public void legacySingleStageDashboardIsReadableTest() {
        MetricAggregation aggregation = readLegacyPrimaryAxes(percentileAggregationDocument(Map.of("pclValue", 90)))
            .getAggregation();
        Assert.assertNull(aggregation.getTwoStageAggregation());
        Assert.assertEquals(MetricAggregationType.PERCENTILE, aggregation.getType());
        Assert.assertEquals(90, aggregation.getParams().get("pclValue"));
    }

    /**
     * Documents written by a development build, which carried the two stages inside the aggregation params, must still
     * deserialize. The stray params are ignored and the aggregation falls back to single-stage.
     */
    @Test
    public void dashboardWithStrayAggregationParamsIsReadableTest() {
        MetricAggregation aggregation = readLegacyPrimaryAxes(
            percentileAggregationDocument(Map.of("timeAggregation", "AVG", "groupAggregation", "SUM")))
            .getAggregation();
        Assert.assertNull(aggregation.getTwoStageAggregation());
        Assert.assertEquals(MetricAggregationType.PERCENTILE, aggregation.getType());
    }

    private DocumentObject percentileAggregationDocument(Map<String, Object> params) {
        DocumentObject aggregation = new DocumentObject();
        aggregation.put("type", MetricAggregationType.PERCENTILE.name());
        aggregation.put("params", params);
        return aggregation;
    }

    /** Writes a raw dashboard document and reads it back through the typed collection, as the controller does. */
    private AxesSettings readLegacyPrimaryAxes(DocumentObject aggregation) {
        CollectionFactory collectionFactory = new InMemoryCollectionFactory(null);
        DocumentObject primaryAxes = new DocumentObject();
        primaryAxes.put("aggregation", aggregation);
        primaryAxes.put("displayType", AxesDisplayType.LINE.name());
        primaryAxes.put("unit", "ms");
        DocumentObject chartSettings = new DocumentObject();
        chartSettings.put("primaryAxes", primaryAxes);
        DocumentObject dashlet = new DocumentObject();
        dashlet.put("id", "dashlet-1");
        dashlet.put("name", "Legacy dashlet");
        dashlet.put("type", DashletType.CHART.name());
        dashlet.put("metricKey", RESPONSE_TIME);
        dashlet.put("chartSettings", chartSettings);
        Document dashboard = new Document();
        dashboard.put("attributes", Map.of(AbstractOrganizableObject.NAME, "Legacy dashboard"));
        dashboard.put("dashlets", List.of(dashlet));
        collectionFactory.getCollection("dashboards", Document.class).save(dashboard);

        DashboardView found = collectionFactory.getCollection("dashboards", DashboardView.class)
            .find(Filters.empty(), null, null, null, 0).findFirst().orElseThrow();
        return found.getDashlets().get(0).getChartSettings().getPrimaryAxes();
    }

    private DashboardView saveAndReload(DashboardView dashboard) {
        DashboardView saved = dashboardAccessor.save(dashboard);
        return dashboardAccessor.findByIds(List.of(saved.getId().toString())).findFirst().orElseThrow();
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

        DashboardView dashboardView = new DashboardView()
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
                    .setAttributes(Arrays.asList(nameAttribute, taskAttribute, executionAttribute, planAttribute))
                    .setMetricKey("response-time")
                    .setInheritGlobalFilters(true)
                    .setGrouping(Arrays.asList("name"))
                    .setChartSettings(new ChartSettings()
                        .setPrimaryAxes(new AxesSettings()
                            .setAggregation(new MetricAggregation(MetricAggregationType.AVG))
                            .setDisplayType(AxesDisplayType.LINE)
                            .setUnit("ms")
                        )
                    ),
                new DashboardItem()
                    .setName("Executions count")
                    .setType(DashletType.CHART)
                    .setMetricKey(EXECUTIONS_COUNT)
                    .setInheritGlobalFilters(false)
                    .setGrouping(List.of())
                    .setAttributes(Arrays.asList(taskAttribute, executionAttribute, planAttribute))
                    .setChartSettings(new ChartSettings()
                        .setPrimaryAxes(new AxesSettings()
                            .setAggregation(new MetricAggregation(MetricAggregationType.SUM))
                            .setDisplayType(AxesDisplayType.LINE)
                            .setUnit("1")
                        )
                    ),
                new DashboardItem()
                    .setName("Statuses")
                    .setType(DashletType.CHART)
                    .setMetricKey(RESPONSE_TIME)
                    .setInheritGlobalFilters(false)
                    .setGrouping(List.of("rnStatus"))
                    .setReadonlyGrouping(true)
                    .setAttributes(Arrays.asList(nameAttribute, taskAttribute, executionAttribute, planAttribute))
                    .setChartSettings(new ChartSettings()
                        .setPrimaryAxes(new AxesSettings()
                            .setAggregation(new MetricAggregation(MetricAggregationType.COUNT))
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
        dashboardView.addAttribute(AbstractOrganizableObject.NAME, "Initial Dashboard");
        return dashboardView;
    }

}
