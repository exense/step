package step.plugins.timeseries.dashboards;

import step.controller.grid.GridPlugin;
import step.core.deployment.ControllerServiceException;
import step.core.metrics.InstrumentType;
import step.core.timeseries.metric.MetricAggregation;
import step.core.timeseries.metric.MetricAggregationType;
import step.core.timeseries.metric.MetricAttribute;
import step.core.timeseries.metric.MetricType;
import step.plugins.timeseries.TimeSeriesControllerPlugin;
import step.plugins.timeseries.dashboards.model.*;

import java.util.*;

import static step.controller.services.entities.AbstractEntityServices.CUSTOM_FIELD_LOCKED;
import static step.core.metrics.MetricsConstants.*;
import static step.core.metrics.MetricsControllerPlugin.*;
import static step.plugins.timeseries.TimeSeriesControllerPlugin.GENERATION_NAME;


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
        MetricType histogramMetric = getMetricByName(metricsByNames, InstrumentType.HISTOGRAM.toLowerCase());
        MetricType gaugeMetric = getMetricByName(metricsByNames, InstrumentType.GAUGE.toLowerCase());
        MetricType counterMetric = getMetricByName(metricsByNames, InstrumentType.COUNTER.toLowerCase());

        DashboardItem tableDashlet = createTableDashlet(responseTimeMetric);
        DashboardItem responseTimeDashlet = createResponseTimeDashlet(responseTimeMetric);
        DashboardItem throughputDashlet = createThroughputDashlet(responseTimeMetric);
        DashboardItem histogramTableDashlet = createCustomMetricTableDashlet(histogramMetric, getHistogramColumns());
        DashboardItem gaugeTableDashlet = createCustomMetricTableDashlet(gaugeMetric, getGridMonitoringColumns());
        DashboardItem counterTableDashlet = createCustomMetricTableDashlet(counterMetric, getCounterColumns());

        responseTimeDashlet.setMasterChartId(tableDashlet.getId());
        throughputDashlet.setMasterChartId(tableDashlet.getId());


        DashboardView dashboard = new DashboardView();
        dashboard.addCustomField(CUSTOM_FIELD_LOCKED, true);
        dashboard.addCustomField(GENERATION_NAME, TimeSeriesControllerPlugin.EXECUTION_DASHBOARD_PREPOPULATED_NAME);
        dashboard
            .setGrouping(List.of("name"))
            .setDescription("Preconfigured dashboard for execution performance analysis")
            .setTimeRange(new TimeRangeSelection().setType(TimeRangeSelectionType.FULL))
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
                createThreadGroupDashlet(threadGroupMetric),
                histogramTableDashlet,
                createCustomMetricChartDashlet(histogramMetric, histogramTableDashlet.getId(), new MetricAggregation(MetricAggregationType.AVG), ""),
                gaugeTableDashlet,
                createCustomMetricChartDashlet(gaugeMetric, gaugeTableDashlet.getId(), new MetricAggregation(MetricAggregationType.AVG), "1"),
                counterTableDashlet,
                createCustomMetricChartDashlet(counterMetric, counterTableDashlet.getId(), new MetricAggregation(MetricAggregationType.RATE, Map.of(RATE_UNIT_KEY, "s")), "1")
            ))
            .addAttribute("name", TimeSeriesControllerPlugin.EXECUTION_DASHBOARD_PREPOPULATED_NAME);

        return dashboard;
    }

    public DashboardView createAnalyticsDashboard() {
        MetricType responseTimeMetric = getMetricByName(metricsByNames, RESPONSE_TIME);
        MetricType threadGroupMetric = getMetricByName(metricsByNames, THREAD_GROUP);
        MetricType histogramMetric = getMetricByName(metricsByNames, InstrumentType.HISTOGRAM.toLowerCase());
        MetricType gaugeMetric = getMetricByName(metricsByNames, InstrumentType.GAUGE.toLowerCase());
        MetricType counterMetric = getMetricByName(metricsByNames, InstrumentType.COUNTER.toLowerCase());

        DashboardItem tableDashlet = createTableDashlet(responseTimeMetric);
        DashboardItem responseTimeDashlet = createResponseTimeDashlet(responseTimeMetric);
        DashboardItem throughputDashlet = createThroughputDashlet(responseTimeMetric);
        DashboardItem histogramTableDashlet = createCustomMetricTableDashlet(histogramMetric, getHistogramColumns());
        DashboardItem gaugeTableDashlet = createCustomMetricTableDashlet(gaugeMetric, getGridMonitoringColumns());
        DashboardItem counterTableDashlet = createCustomMetricTableDashlet(counterMetric, getCounterColumns());

        responseTimeDashlet.setMasterChartId(tableDashlet.getId());
        throughputDashlet.setMasterChartId(tableDashlet.getId());


        DashboardView dashboard = new DashboardView();
        dashboard.addCustomField(CUSTOM_FIELD_LOCKED, true);
        dashboard.addCustomField(GENERATION_NAME, TimeSeriesControllerPlugin.ANALYTICS_DASHBOARD_PREPOPULATED_NAME);
        dashboard
            .setGrouping(List.of("name"))
            .setDescription("Preconfigured dashboard for scheduled executions analysis")
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
                createThreadGroupDashlet(threadGroupMetric),
                histogramTableDashlet,
                createCustomMetricChartDashlet(histogramMetric, histogramTableDashlet.getId(), new MetricAggregation(MetricAggregationType.AVG), ""),
                gaugeTableDashlet,
                createCustomMetricChartDashlet(gaugeMetric, gaugeTableDashlet.getId(), new MetricAggregation(MetricAggregationType.AVG), "1"),
                counterTableDashlet,
                createCustomMetricChartDashlet(counterMetric, counterTableDashlet.getId(), new MetricAggregation(MetricAggregationType.RATE, Map.of(RATE_UNIT_KEY, "s")), "1")
            ))
            .addAttribute("name", TimeSeriesControllerPlugin.ANALYTICS_DASHBOARD_PREPOPULATED_NAME);

        return dashboard;
    }


    public DashboardView createExecutionsOverviewDashboard() {
        MetricType durationMetric = getMetricByName(metricsByNames, EXECUTIONS_DURATION);
        MetricType countMetric = getMetricByName(metricsByNames, EXECUTIONS_COUNT);
        MetricType failurePercentageMetric = getMetricByName(metricsByNames, FAILURE_PERCENTAGE);
        MetricType failureCountMetric = getMetricByName(metricsByNames, FAILURE_COUNT);
        MetricType failureCountByErrorCodeMetric = getMetricByName(metricsByNames, FAILURES_COUNT_BY_ERROR_CODE);

        DashboardView dashboard = new DashboardView();
        dashboard.addCustomField(CUSTOM_FIELD_LOCKED, true);
        dashboard.addCustomField(GENERATION_NAME, TimeSeriesControllerPlugin.EXECUTIONS_OVERVIEW_DASHBOARD_PREPOPULATED_NAME);
        dashboard
            .setGrouping(List.of(PLAN_ATTRIBUTE.getName()))
            .setDescription("Preconfigured dashboard for executions overview")
            .setTimeRange(new TimeRangeSelection().setType(TimeRangeSelectionType.RELATIVE).setRelativeSelection(new TimeRangeRelativeSelection().setTimeInMs(60000L * 60)))
            .setFilters(Arrays.asList(
                emptyFilterFromAttribute(TASK_ATTRIBUTE, TimeSeriesFilterItemType.TASK, false),
                emptyFilterFromAttribute(EXECUTION_ATTRIBUTE, TimeSeriesFilterItemType.EXECUTION, false),
                emptyFilterFromAttribute(PLAN_ATTRIBUTE, TimeSeriesFilterItemType.PLAN, false),
                emptyFilterFromAttribute(EXECUTION_BOOLEAN_RESULT, TimeSeriesFilterItemType.FREE_TEXT, false),
                emptyFilterFromAttribute(EXECUTION_RESULT, TimeSeriesFilterItemType.FREE_TEXT, false),
                emptyFilterFromAttribute(ERROR_CODE_ATTRIBUTE, TimeSeriesFilterItemType.FREE_TEXT, false)
            ))
            .setDashlets(Arrays.asList(
                new DashboardItem()
                    .setName("Execution duration")
                    .setType(DashletType.CHART)
                    .setGrouping(Arrays.asList(EXECUTION_ATTRIBUTE.getName(), EXECUTION_BOOLEAN_RESULT.getName()))
                    .setAttributes(durationMetric.getAttributes())
                    .setFilters(Collections.emptyList())
                    .setMetricKey(EXECUTIONS_DURATION)
                    .setInheritGlobalFilters(true)
                    .setInheritGlobalGrouping(true)
                    .setReadonlyAggregate(false)
                    .setReadonlyGrouping(false)
                    .setSize(1)
                    .setChartSettings(new ChartSettings()
                        .setPrimaryAxes(new AxesSettings()
                            .setAggregation(new MetricAggregation(MetricAggregationType.AVG))
                            .setUnit("ms")
                            .setDisplayType(AxesDisplayType.LINE)
                            .setColorizationType(AxesColorizationType.STROKE))),
                new DashboardItem()
                    .setName("Execution throughput")
                    .setType(DashletType.CHART)
                    .setGrouping(Collections.emptyList())
                    .setAttributes(durationMetric.getAttributes())
                    .setFilters(Collections.emptyList())
                    .setMetricKey(EXECUTIONS_DURATION)
                    .setInheritGlobalFilters(true)
                    .setInheritGlobalGrouping(true)
                    .setReadonlyAggregate(false)
                    .setReadonlyGrouping(false)
                    .setSize(1)
                    .setChartSettings(new ChartSettings()
                        .setPrimaryAxes(new AxesSettings()
                            .setAggregation(new MetricAggregation(MetricAggregationType.RATE, Map.of(RATE_UNIT_KEY, "h")))
                            .setUnit("ms")
                            .setDisplayType(AxesDisplayType.LINE)
                            .setColorizationType(AxesColorizationType.STROKE))),
                new DashboardItem()
                    .setName("Execution count")
                    .setType(DashletType.CHART)
                    .setGrouping(List.of(EXECUTION_ATTRIBUTE.getName()))
                    .setAttributes(countMetric.getAttributes())
                    .setFilters(Collections.emptyList())
                    .setMetricKey(EXECUTIONS_COUNT)
                    .setInheritGlobalFilters(true)
                    .setInheritGlobalGrouping(true)
                    .setReadonlyAggregate(false)
                    .setReadonlyGrouping(false)
                    .setSize(1)
                    .setChartSettings(new ChartSettings()
                        .setPrimaryAxes(new AxesSettings()
                            .setAggregation(new MetricAggregation(MetricAggregationType.SUM))
                            .setUnit("1")
                            .setDisplayType(AxesDisplayType.BAR_CHART)
                            .setColorizationType(AxesColorizationType.STROKE))),
                new DashboardItem()
                    .setName("Execution failure count")
                    .setType(DashletType.CHART)
                    .setGrouping(List.of(EXECUTION_ATTRIBUTE.getName()))
                    .setAttributes(failureCountMetric.getAttributes())
                    .setFilters(Collections.emptyList())
                    .setMetricKey(FAILURE_COUNT)
                    .setInheritGlobalFilters(true)
                    .setInheritGlobalGrouping(true)
                    .setReadonlyAggregate(false)
                    .setReadonlyGrouping(false)
                    .setSize(1)
                    .setChartSettings(new ChartSettings()
                        .setPrimaryAxes(new AxesSettings()
                            .setAggregation(new MetricAggregation(MetricAggregationType.SUM))
                            .setUnit("1")
                            .setDisplayType(AxesDisplayType.BAR_CHART)
                            .setColorizationType(AxesColorizationType.STROKE))),
                new DashboardItem()
                    .setName("Execution failure percentage")
                    .setType(DashletType.CHART)
                    .setGrouping(Collections.emptyList())
                    .setAttributes(failurePercentageMetric.getAttributes())
                    .setFilters(Collections.emptyList())
                    .setMetricKey(FAILURE_PERCENTAGE)
                    .setInheritGlobalFilters(true)
                    .setInheritGlobalGrouping(true)
                    .setReadonlyAggregate(false)
                    .setReadonlyGrouping(false)
                    .setSize(1)
                    .setChartSettings(new ChartSettings()
                        .setPrimaryAxes(new AxesSettings()
                            .setAggregation(new MetricAggregation(MetricAggregationType.AVG))
                            .setUnit("%")
                            .setDisplayType(AxesDisplayType.BAR_CHART)
                            .setColorizationType(AxesColorizationType.FILL))),
                new DashboardItem()
                    .setName("Execution failure count by error code")
                    .setType(DashletType.CHART)
                    .setGrouping(List.of(ERROR_CODE_ATTRIBUTE.getName()))
                    .setAttributes(failureCountByErrorCodeMetric.getAttributes())
                    .setFilters(Collections.emptyList())
                    .setMetricKey(FAILURES_COUNT_BY_ERROR_CODE)
                    .setInheritGlobalFilters(true)
                    .setInheritGlobalGrouping(true)
                    .setReadonlyAggregate(false)
                    .setReadonlyGrouping(false)
                    .setSize(1)
                    .setChartSettings(new ChartSettings()
                        .setPrimaryAxes(new AxesSettings()
                            .setAggregation(new MetricAggregation(MetricAggregationType.SUM))
                            .setUnit("1")
                            .setDisplayType(AxesDisplayType.BAR_CHART)
                            .setColorizationType(AxesColorizationType.STROKE))),
                new DashboardItem()
                    .setId(UUID.randomUUID().toString())
                    .setName("Execution duration")
                    .setType(DashletType.TABLE)
                    .setGrouping(Arrays.asList(PLAN_ATTRIBUTE.getName(), EXECUTION_BOOLEAN_RESULT.getName()))
                    .setAttributes(durationMetric.getAttributes())
                    .setFilters(Collections.emptyList())
                    .setMetricKey(EXECUTIONS_DURATION)
                    .setInheritGlobalFilters(true)
                    .setInheritGlobalGrouping(false)
                    .setReadonlyAggregate(true)
                    .setReadonlyGrouping(true)
                    .setSize(2)
                    .setTableSettings(new TableDashletSettings()
                        .setColumns(getExecutionsOverviewColumns()))
            ))
            .addAttribute("name", TimeSeriesControllerPlugin.EXECUTIONS_OVERVIEW_DASHBOARD_PREPOPULATED_NAME);

        return dashboard;
    }

    private static List<TableDashletSettings.ColumnSelection> getExecutionsOverviewColumns() {
        return Arrays.asList(
            new TableDashletSettings.ColumnSelection(TableChartColumn.COUNT, new MetricAggregation(MetricAggregationType.COUNT)),
            new TableDashletSettings.ColumnSelection(TableChartColumn.SUM, new MetricAggregation(MetricAggregationType.SUM), false),
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

    public DashboardView createGridMonitoringDashboard() {
        MetricType capacityMetric = getMetricByName(metricsByNames, GridPlugin.GRID_CAPACITY_METRIC_NAME);
        MetricType byStateMetric = getMetricByName(metricsByNames, GridPlugin.GRID_BY_STATE_METRIC_NAME);

        DashboardView dashboard = new DashboardView();
        dashboard.addCustomField(CUSTOM_FIELD_LOCKED, true);
        dashboard.addCustomField(GENERATION_NAME, TimeSeriesControllerPlugin.GRID_MONITORING_DASHBOARD_PREPOPULATED_NAME);
        dashboard
            .setGrouping(List.of(GRID_TOKEN_AGENT_TYPE.getName(), GRID_TOKEN_STATE.getName()))
            .setDescription("Preconfigured dashboard for grid monitoring")
            .setTimeRange(new TimeRangeSelection().setType(TimeRangeSelectionType.RELATIVE).setRelativeSelection(new TimeRangeRelativeSelection().setTimeInMs(60000L * 60)))
            .setFilters(Arrays.asList(
                emptyFilterFromAttribute(GRID_TOKEN_AGENT_TYPE, TimeSeriesFilterItemType.FREE_TEXT, false),
                emptyFilterFromAttribute(GRID_TOKEN_STATE, TimeSeriesFilterItemType.FREE_TEXT, false)
            ))
            .setDashlets(Arrays.asList(
                createGridMetricChartDashlet(capacityMetric, "Grid tokens capacity"),
                createGridMetricChartDashlet(byStateMetric, "Grid tokens by state")//,
                //Tables not added for now, since we do not create samples when the agents are down, the stats can be confusing
                //createGridMetricTableDashlet(capacityMetric, "Grid tokens capacity"),
                //createGridMetricTableDashlet(byStateMetric, "Grid tokens by state")
            ))
            .addAttribute("name", TimeSeriesControllerPlugin.GRID_MONITORING_DASHBOARD_PREPOPULATED_NAME);

        return dashboard;
    }

    private static DashboardItem createGridMetricChartDashlet(MetricType metric, String name) {
        return new DashboardItem()
            .setName(name)
            .setType(DashletType.CHART)
            .setGrouping(metric.getDefaultGroupingAttributes())
            .setAttributes(metric.getAttributes())
            .setFilters(Collections.emptyList())
            .setMetricKey(metric.getName())
            .setInheritGlobalFilters(true)
            .setInheritGlobalGrouping(true)
            .setReadonlyAggregate(false)
            .setReadonlyGrouping(false)
            .setSize(2)
            .setChartSettings(new ChartSettings()
                .setPrimaryAxes(new AxesSettings()
                    .setAggregation(new MetricAggregation(MetricAggregationType.AVG))
                    .setUnit("1")
                    .setDisplayType(AxesDisplayType.LINE)
                    .setColorizationType(AxesColorizationType.FILL)));
    }

    private static DashboardItem createGridMetricTableDashlet(MetricType metric, String name) {
        return new DashboardItem()
            .setId(UUID.randomUUID().toString())
            .setName(name)
            .setType(DashletType.TABLE)
            .setGrouping(metric.getDefaultGroupingAttributes())
            .setAttributes(metric.getAttributes())
            .setFilters(Collections.emptyList())
            .setMetricKey(metric.getName())
            .setInheritGlobalFilters(true)
            .setInheritGlobalGrouping(true)
            .setReadonlyAggregate(true)
            .setReadonlyGrouping(true)
            .setSize(1)
            .setTableSettings(new TableDashletSettings()
                .setColumns(getGridMonitoringColumns()));
    }

    private static List<TableDashletSettings.ColumnSelection> getGridMonitoringColumns() {
        return Arrays.asList(
            new TableDashletSettings.ColumnSelection(TableChartColumn.COUNT, new MetricAggregation(MetricAggregationType.COUNT), false),
            new TableDashletSettings.ColumnSelection(TableChartColumn.SUM, new MetricAggregation(MetricAggregationType.SUM), false),
            new TableDashletSettings.ColumnSelection(TableChartColumn.AVG, new MetricAggregation(MetricAggregationType.AVG)),
            new TableDashletSettings.ColumnSelection(TableChartColumn.MIN, new MetricAggregation(MetricAggregationType.MIN)),
            new TableDashletSettings.ColumnSelection(TableChartColumn.MAX, new MetricAggregation(MetricAggregationType.MAX)),
            new TableDashletSettings.ColumnSelection(TableChartColumn.PCL_1, new MetricAggregation(MetricAggregationType.PERCENTILE, Map.of(PCL_VALUE_KEY, 80D)), false),
            new TableDashletSettings.ColumnSelection(TableChartColumn.PCL_2, new MetricAggregation(MetricAggregationType.PERCENTILE, Map.of(PCL_VALUE_KEY, 90D)), false),
            new TableDashletSettings.ColumnSelection(TableChartColumn.PCL_3, new MetricAggregation(MetricAggregationType.PERCENTILE, Map.of(PCL_VALUE_KEY, 99D)), false),
            new TableDashletSettings.ColumnSelection(TableChartColumn.TPS, new MetricAggregation(MetricAggregationType.RATE, Map.of(RATE_UNIT_KEY, "s")), false),
            new TableDashletSettings.ColumnSelection(TableChartColumn.TPH, new MetricAggregation(MetricAggregationType.RATE, Map.of(RATE_UNIT_KEY, "h")), false)
        );
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
                    .setAggregation(new MetricAggregation(MetricAggregationType.RATE, Map.of(RATE_UNIT_KEY, "s")))
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
            .setReadonlyAggregate(true)
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
            .setSize(2)
            .setChartSettings(new ChartSettings()
                .setPrimaryAxes(new AxesSettings()
                    .setAggregation(new MetricAggregation(MetricAggregationType.MAX))
                    .setUnit("1")
                    .setDisplayType(AxesDisplayType.LINE)
                    .setColorizationType(AxesColorizationType.STROKE))
                .setSecondaryAxes(new AxesSettings().setAggregation(new MetricAggregation(MetricAggregationType.MAX))));
    }

    private static DashboardItem createCustomMetricTableDashlet(MetricType metric, List<TableDashletSettings.ColumnSelection> columns) {
        return new DashboardItem()
            .setId(UUID.randomUUID().toString())
            .setName(metric.getDisplayName())
            .setType(DashletType.TABLE)
            .setGrouping(metric.getDefaultGroupingAttributes())
            .setAttributes(metric.getAttributes())
            .setFilters(Collections.emptyList())
            .setMetricKey(metric.getName())
            .setInheritGlobalFilters(true)
            .setInheritGlobalGrouping(true)
            .setReadonlyAggregate(true)
            .setReadonlyGrouping(true)
            .setOmitWhenEmpty(true)
            .setSize(1)
            .setTableSettings(new TableDashletSettings().setColumns(columns));
    }

    private static DashboardItem createCustomMetricChartDashlet(MetricType metric, String masterChartId, MetricAggregation aggregation, String unit) {
        return new DashboardItem()
            .setName(metric.getDisplayName())
            .setType(DashletType.CHART)
            .setMasterChartId(masterChartId)
            .setGrouping(metric.getDefaultGroupingAttributes())
            .setAttributes(metric.getAttributes())
            .setFilters(Collections.emptyList())
            .setMetricKey(metric.getName())
            .setInheritGlobalFilters(true)
            .setInheritGlobalGrouping(true)
            .setReadonlyAggregate(false)
            .setReadonlyGrouping(false)
            .setOmitWhenEmpty(true)
            .setSize(1)
            .setChartSettings(new ChartSettings()
                .setPrimaryAxes(new AxesSettings()
                    .setAggregation(aggregation)
                    .setUnit(unit)
                    .setDisplayType(AxesDisplayType.LINE)
                    .setColorizationType(AxesColorizationType.STROKE)));
    }

    private static List<TableDashletSettings.ColumnSelection> getHistogramColumns() {
        return Arrays.asList(
            new TableDashletSettings.ColumnSelection(TableChartColumn.COUNT, new MetricAggregation(MetricAggregationType.COUNT)),
            new TableDashletSettings.ColumnSelection(TableChartColumn.SUM, new MetricAggregation(MetricAggregationType.SUM), false),
            new TableDashletSettings.ColumnSelection(TableChartColumn.AVG, new MetricAggregation(MetricAggregationType.AVG)),
            new TableDashletSettings.ColumnSelection(TableChartColumn.MIN, new MetricAggregation(MetricAggregationType.MIN), false),
            new TableDashletSettings.ColumnSelection(TableChartColumn.MAX, new MetricAggregation(MetricAggregationType.MAX), false),
            new TableDashletSettings.ColumnSelection(TableChartColumn.PCL_1, new MetricAggregation(MetricAggregationType.PERCENTILE, Map.of(PCL_VALUE_KEY, 80D))),
            new TableDashletSettings.ColumnSelection(TableChartColumn.PCL_2, new MetricAggregation(MetricAggregationType.PERCENTILE, Map.of(PCL_VALUE_KEY, 90D))),
            new TableDashletSettings.ColumnSelection(TableChartColumn.PCL_3, new MetricAggregation(MetricAggregationType.PERCENTILE, Map.of(PCL_VALUE_KEY, 99D)), false),
            new TableDashletSettings.ColumnSelection(TableChartColumn.TPS, new MetricAggregation(MetricAggregationType.RATE, Map.of(RATE_UNIT_KEY, "s"))),
            new TableDashletSettings.ColumnSelection(TableChartColumn.TPH, new MetricAggregation(MetricAggregationType.RATE, Map.of(RATE_UNIT_KEY, "h")))
        );
    }

    private static List<TableDashletSettings.ColumnSelection> getCounterColumns() {
        return Arrays.asList(
            new TableDashletSettings.ColumnSelection(TableChartColumn.COUNT, new MetricAggregation(MetricAggregationType.COUNT), false),
            new TableDashletSettings.ColumnSelection(TableChartColumn.SUM, new MetricAggregation(MetricAggregationType.SUM)),
            new TableDashletSettings.ColumnSelection(TableChartColumn.AVG, new MetricAggregation(MetricAggregationType.AVG), false),
            new TableDashletSettings.ColumnSelection(TableChartColumn.MIN, new MetricAggregation(MetricAggregationType.MIN), false),
            new TableDashletSettings.ColumnSelection(TableChartColumn.MAX, new MetricAggregation(MetricAggregationType.MAX)),
            new TableDashletSettings.ColumnSelection(TableChartColumn.PCL_1, new MetricAggregation(MetricAggregationType.PERCENTILE, Map.of(PCL_VALUE_KEY, 80D)), false),
            new TableDashletSettings.ColumnSelection(TableChartColumn.PCL_2, new MetricAggregation(MetricAggregationType.PERCENTILE, Map.of(PCL_VALUE_KEY, 90D)), false),
            new TableDashletSettings.ColumnSelection(TableChartColumn.PCL_3, new MetricAggregation(MetricAggregationType.PERCENTILE, Map.of(PCL_VALUE_KEY, 99D)), false),
            new TableDashletSettings.ColumnSelection(TableChartColumn.TPS, new MetricAggregation(MetricAggregationType.RATE, Map.of(RATE_UNIT_KEY, "s"))),
            new TableDashletSettings.ColumnSelection(TableChartColumn.TPH, new MetricAggregation(MetricAggregationType.RATE, Map.of(RATE_UNIT_KEY, "h")))
        );
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
