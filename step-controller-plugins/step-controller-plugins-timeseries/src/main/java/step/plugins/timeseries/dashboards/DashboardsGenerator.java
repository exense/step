package step.plugins.timeseries.dashboards;

import step.core.deployment.ControllerServiceException;
import step.core.timeseries.metric.MetricAggregation;
import step.core.timeseries.metric.MetricAggregationType;
import step.core.timeseries.metric.MetricAttribute;
import step.core.timeseries.metric.MetricType;
import step.plugins.timeseries.TimeSeriesControllerPlugin;
import step.plugins.timeseries.dashboards.model.*;

import java.util.*;
import java.util.stream.Collectors;

import static step.controller.services.entities.AbstractEntityServices.CUSTOM_FIELD_LOCKED;
import static step.plugins.timeseries.MetricsConstants.*;
import static step.plugins.timeseries.TimeSeriesControllerPlugin.GENERATION_NAME;
import static step.plugins.timeseries.TimeSeriesExecutionPlugin.RESPONSE_TIME;
import static step.plugins.timeseries.TimeSeriesExecutionPlugin.THREAD_GROUP;

public class DashboardsGenerator {

    private final static String PCL_VALUE_KEY = "pclValue";
    private final static String RATE_UNIT_KEY = "rateUnit";
    
    private final Map<String, MetricType> metricsByNames = new HashMap<>();

    public DashboardsGenerator(List<MetricType> metrics) {
        metrics.forEach(metricType -> metricsByNames.put(metricType.getName(), metricType));
    }

    private MetricType getMetricByName(Map<String, MetricType> metricsByNames, String metric) {
        if (metricsByNames.containsKey(metric)) {
            return metricsByNames.get(metric);
        } else {
            throw new ControllerServiceException("Metric by name not found: " + metric);
        }
    }

    private static TimeSeriesFilterItem emptyFilterFromAttribute(MetricAttribute attribute, TimeSeriesFilterItemType type, boolean exactMatch) {
        List<String> textOptions = null;
        Map<String, Object> metadata = attribute.getMetadata();
        if (metadata != null) {
            if (metadata.get("knownValues") instanceof List) {
                textOptions = (List<String>) metadata.get("knownValues");
            }
        }
        return new TimeSeriesFilterItem()
                .setAttribute(attribute.getName())
                .setLabel(attribute.getDisplayName())
                .setTextOptions(textOptions)
                .setType(type)
                .setExactMatch(exactMatch)
                .setRemovable(false);
    }
    
    public DashboardView createExecutionDashboard() {
        MetricType responseTimeMetric = getMetricByName(metricsByNames, RESPONSE_TIME);
        MetricType threadGroupMetric = getMetricByName(metricsByNames, THREAD_GROUP);

        DashboardItem tableDashlet = createTableDashlet(responseTimeMetric);
        DashboardItem responseTimeDashlet = createResponseTimeDashlet(responseTimeMetric);
        DashboardItem throughputDashlet = createThroughputDashlet(responseTimeMetric);

        responseTimeDashlet.setMasterChartId(tableDashlet.getId());
        throughputDashlet.setMasterChartId(tableDashlet.getId());


        DashboardView dashboard = new DashboardView();
        dashboard.addCustomField(CUSTOM_FIELD_LOCKED, true);
        dashboard.addCustomField(GENERATION_NAME, TimeSeriesControllerPlugin.EXECUTION_DASHBOARD_PREPOPULATED_NAME);
        dashboard
                .setGrouping(List.of("name"))
                .setDescription("Readonly default dashboard used for analytics display")
                .setTimeRange(new TimeRangeSelection().setType(TimeRangeSelectionType.RELATIVE).setRelativeSelection(new TimeRangeRelativeSelection().setTimeInMs(60000L)))
                .setFilters(Arrays.asList(
                        emptyFilterFromAttribute(STATUS_ATTRIBUTE, TimeSeriesFilterItemType.OPTIONS, true),
                        emptyFilterFromAttribute(TYPE_ATRIBUTE, TimeSeriesFilterItemType.OPTIONS, true),
                        emptyFilterFromAttribute(NAME_ATTRIBUTE, TimeSeriesFilterItemType.FREE_TEXT, false)
                ))
                .setDashlets(Arrays.asList(
                        createSummaryDashlet(responseTimeMetric),
                        createStatusDashlet(responseTimeMetric),
                        responseTimeDashlet,
                        throughputDashlet,
                        tableDashlet,
                        createThreadGroupDashlet(threadGroupMetric)
                ))
                .addAttribute("name", TimeSeriesControllerPlugin.EXECUTION_DASHBOARD_PREPOPULATED_NAME);

        return dashboard;
    }

    public DashboardView createAnalyticsDashboard() {
        MetricType responseTimeMetric = getMetricByName(metricsByNames, RESPONSE_TIME);
        MetricType threadGroupMetric = getMetricByName(metricsByNames, THREAD_GROUP);


        DashboardItem tableDashlet = createTableDashlet(responseTimeMetric);
        DashboardItem responseTimeDashlet = createResponseTimeDashlet(responseTimeMetric);
        DashboardItem throughputDashlet = createThroughputDashlet(responseTimeMetric);

        responseTimeDashlet.setMasterChartId(tableDashlet.getId());
        throughputDashlet.setMasterChartId(tableDashlet.getId());


        DashboardView dashboard = new DashboardView();
        dashboard.addCustomField(CUSTOM_FIELD_LOCKED, true);
        dashboard.addCustomField(GENERATION_NAME, TimeSeriesControllerPlugin.ANALYTICS_DASHBOARD_PREPOPULATED_NAME);
        dashboard
                .setGrouping(List.of("name"))
                .setDescription("Readonly default dashboard used for executions display")
                .setTimeRange(new TimeRangeSelection().setType(TimeRangeSelectionType.RELATIVE).setRelativeSelection(new TimeRangeRelativeSelection().setTimeInMs(60000L * 60)))
                .setFilters(Arrays.asList(
                        emptyFilterFromAttribute(STATUS_ATTRIBUTE, TimeSeriesFilterItemType.OPTIONS, true),
                        emptyFilterFromAttribute(TYPE_ATRIBUTE, TimeSeriesFilterItemType.OPTIONS, true),
                        emptyFilterFromAttribute(NAME_ATTRIBUTE, TimeSeriesFilterItemType.FREE_TEXT, false),
                        emptyFilterFromAttribute(EXECUTION_ATTRIBUTE, TimeSeriesFilterItemType.EXECUTION, true),
                        emptyFilterFromAttribute(TASK_ATTRIBUTE, TimeSeriesFilterItemType.TASK, true),
                        emptyFilterFromAttribute(PLAN_ATTRIBUTE, TimeSeriesFilterItemType.PLAN, true)
                ))
                .setDashlets(Arrays.asList(
                        createSummaryDashlet(responseTimeMetric),
                        createStatusDashlet(responseTimeMetric),
                        responseTimeDashlet,
                        throughputDashlet,
                        tableDashlet,
                        createThreadGroupDashlet(threadGroupMetric)
                ))
                .addAttribute("name", TimeSeriesControllerPlugin.ANALYTICS_DASHBOARD_PREPOPULATED_NAME);

        return dashboard;
    }


    private static DashboardItem createTableDashlet(MetricType metric) {
        return new DashboardItem()
                .setId(UUID.randomUUID().toString())
                .setName("Stats")
                .setType(DashletType.TABLE)
                .setGrouping(Collections.emptyList())
                .setAttributes(metric.getAttributes())
                .setFilters(Collections.emptyList())
                .setMetricKey(metric.getName())
                .setInheritGlobalFilters(true)
                .setInheritGlobalGrouping(true)
                .setReadonlyAggregate(true)
                .setReadonlyGrouping(true)
                .setSize(2)
                .setTableSettings(new TableDashletSettings()
                        .setColumns(getFullVisibleColumns()));
    }

    private static DashboardItem createSummaryDashlet(MetricType metric) {
        return new DashboardItem()
                .setName("Performance Overview")
                .setType(DashletType.CHART)
                .setGrouping(Collections.emptyList())
                .setAttributes(metric.getAttributes())
                .setFilters(Collections.emptyList())
                .setMetricKey(metric.getName())
                .setInheritGlobalFilters(true)
                .setInheritGlobalGrouping(false)
                .setReadonlyAggregate(true)
                .setReadonlyGrouping(true)
                .setSize(1)
                .setChartSettings(new ChartSettings()
                        .setPrimaryAxes(new AxesSettings()
                                .setAggregation(new MetricAggregation(MetricAggregationType.AVG))
                                .setUnit("ms")
                                .setDisplayType(AxesDisplayType.LINE)
                                .setColorizationType(AxesColorizationType.STROKE))
                        .setSecondaryAxes(new AxesSettings()
                                .setAggregation(new MetricAggregation(MetricAggregationType.RATE, Map.of(RATE_UNIT_KEY, "h")))
                                .setDisplayType(AxesDisplayType.BAR_CHART)
                        ));
    }

    private static DashboardItem createStatusDashlet(MetricType metric) {
        return new DashboardItem()
                .setName("Statuses")
                .setType(DashletType.CHART)
                .setGrouping(Arrays.asList("rnStatus"))
                .setAttributes(metric.getAttributes())
                .setFilters(Collections.emptyList())
                .setMetricKey(metric.getName())
                .setInheritGlobalFilters(true)
                .setInheritGlobalGrouping(false)
                .setReadonlyAggregate(true)
                .setReadonlyGrouping(true)
                .setSize(1)
                .setChartSettings(new ChartSettings()
                        .setPrimaryAxes(new AxesSettings()
                                .setAggregation(new MetricAggregation(MetricAggregationType.COUNT))
                                .setUnit("1")
                                .setDisplayType(AxesDisplayType.LINE)
                                .setColorizationType(AxesColorizationType.FILL)));
    }

    private static DashboardItem createResponseTimeDashlet(MetricType metric) {
        return new DashboardItem()
                .setName("Response Times")
                .setType(DashletType.CHART)
                .setGrouping(Collections.emptyList())
                .setAttributes(metric.getAttributes())
                .setFilters(Collections.emptyList())
                .setMetricKey(metric.getName())
                .setInheritGlobalFilters(true)
                .setInheritGlobalGrouping(true)
                .setReadonlyAggregate(false)
                .setReadonlyGrouping(true)
                .setSize(1)
                .setChartSettings(new ChartSettings()
                        .setPrimaryAxes(new AxesSettings()
                                .setAggregation(new MetricAggregation(MetricAggregationType.AVG))
                                .setUnit("ms")
                                .setDisplayType(AxesDisplayType.LINE)
                                .setColorizationType(AxesColorizationType.STROKE)));
    }

    private static DashboardItem createThroughputDashlet(MetricType metric) {
        return new DashboardItem()
                .setName("Throughput")
                .setType(DashletType.CHART)
                .setGrouping(Collections.emptyList())
                .setAttributes(metric.getAttributes())
                .setFilters(Collections.emptyList())
                .setMetricKey(metric.getName())
                .setInheritGlobalFilters(true)
                .setInheritGlobalGrouping(true)
                .setReadonlyAggregate(false)
                .setReadonlyGrouping(true)
                .setSize(1)
                .setChartSettings(new ChartSettings()
                        .setPrimaryAxes(new AxesSettings()
                                .setAggregation(new MetricAggregation(MetricAggregationType.RATE, Map.of(RATE_UNIT_KEY, "h")))
                                .setUnit(null)
                                .setDisplayType(AxesDisplayType.LINE)
                                .setColorizationType(AxesColorizationType.STROKE))
                        .setSecondaryAxes(new AxesSettings()
                                .setDisplayType(AxesDisplayType.BAR_CHART)
                                .setAggregation(new MetricAggregation(MetricAggregationType.RATE, Map.of(RATE_UNIT_KEY, "h")))));
    }

    private static DashboardItem createThreadGroupDashlet(MetricType metric) {
        return new DashboardItem()
                .setId(UUID.randomUUID().toString())
                .setName("Thread Groups (Concurrency)")
                .setType(DashletType.CHART)
                .setGrouping(Arrays.asList("name"))
                .setAttributes(metric.getAttributes())
                .setFilters(Collections.emptyList())
                .setMetricKey(metric.getName())
                .setInheritGlobalFilters(true)
                .setInheritGlobalGrouping(false)
                .setReadonlyAggregate(true)
                .setReadonlyGrouping(true)
                .setInheritSpecificFiltersOnly(true)
                .setSpecificFiltersToInherit(Arrays.asList(PLAN_ATTRIBUTE.getName(), EXECUTION_ATTRIBUTE.getName(), TASK_ATTRIBUTE.getName()))
                .setSize(1)
                .setChartSettings(new ChartSettings()
                        .setPrimaryAxes(new AxesSettings()
                                .setAggregation(new MetricAggregation(MetricAggregationType.MAX))
                                .setUnit("1")
                                .setDisplayType(AxesDisplayType.LINE)
                                .setColorizationType(AxesColorizationType.STROKE))
                        .setSecondaryAxes(new AxesSettings().setAggregation(new MetricAggregation(MetricAggregationType.MAX))));
    }


    private static List<TableDashletSettings.ColumnSelection> getFullVisibleColumns() {
        return Arrays.asList(
                new TableDashletSettings.ColumnSelection(TableChartColumn.COUNT, new MetricAggregation(MetricAggregationType.COUNT)),
                new TableDashletSettings.ColumnSelection(TableChartColumn.SUM, new MetricAggregation(MetricAggregationType.SUM)),
                new TableDashletSettings.ColumnSelection(TableChartColumn.AVG, new MetricAggregation(MetricAggregationType.AVG)),
                new TableDashletSettings.ColumnSelection(TableChartColumn.MIN, new MetricAggregation(MetricAggregationType.MIN)),
                new TableDashletSettings.ColumnSelection(TableChartColumn.MAX, new MetricAggregation(MetricAggregationType.MAX)),
                new TableDashletSettings.ColumnSelection(TableChartColumn.PCL_1, new MetricAggregation(MetricAggregationType.PERCENTILE, Map.of(PCL_VALUE_KEY, 80D))),
                new TableDashletSettings.ColumnSelection(TableChartColumn.PCL_2, new MetricAggregation(MetricAggregationType.PERCENTILE, Map.of(PCL_VALUE_KEY, 90D))),
                new TableDashletSettings.ColumnSelection(TableChartColumn.PCL_3, new MetricAggregation(MetricAggregationType.PERCENTILE, Map.of(PCL_VALUE_KEY, 99D))),
                new TableDashletSettings.ColumnSelection(TableChartColumn.TPS, new MetricAggregation(MetricAggregationType.RATE, Map.of(RATE_UNIT_KEY, "s"))),
                new TableDashletSettings.ColumnSelection(TableChartColumn.TPH, new MetricAggregation(MetricAggregationType.RATE, Map.of(RATE_UNIT_KEY, "h")))
        );
    }

}
