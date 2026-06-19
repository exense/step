package step.plugins.timeseries;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.controller.services.async.AsyncTaskManager;
import step.controller.services.async.AsyncTaskManagerPlugin;
import step.core.Constants;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.IndexField;
import step.core.collections.Order;
import step.core.deployment.WebApplicationConfigurationManager;
import step.core.entities.Entity;
import step.core.entities.EntityConstants;
import step.core.execution.model.ExecutionNoticeSeverity;
import step.core.execution.notices.ExecutionNoticeManager;
import step.core.execution.notices.ExecutionNoticeType;
import step.core.metrics.MetricsConstants;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.plugins.exceptions.PluginCriticalException;
import step.core.timeseries.*;
import step.core.timeseries.aggregation.TimeSeriesAggregationPipeline;
import step.core.timeseries.ingestion.TimeSeriesIngestionPipeline;
import step.engine.plugins.ExecutionEnginePlugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.migration.MigrationManager;
import step.migration.MigrationManagerPlugin;
import step.core.metrics.MetricSamplerRegistry;
import step.core.metrics.MetricTypeRegistry;
import step.core.metrics.MetricsControllerPlugin;
import step.core.metrics.MetricsExecutionPlugin;
import step.plugins.timeseries.dashboards.DashboardAccessor;
import step.plugins.timeseries.dashboards.DashboardsGenerator;
import step.plugins.timeseries.dashboards.model.DashboardView;
import step.plugins.timeseries.migration.MigrateAggregateTask;
import step.plugins.timeseries.migration.MigrateDashboardsTask;
import step.plugins.timeseries.migration.MigrateResolutionsWithIgnoredFieldsTask;

import java.util.*;
import java.util.stream.Collectors;

import static step.core.metrics.MetricsExecutionPlugin.AGENT_URL;
import static step.core.metrics.MetricsExecutionPlugin.BEGIN;
import static step.core.metrics.MetricsExecutionPlugin.RN_ID;
import static step.core.metrics.MetricsExecutionPlugin.VALUE;
import static step.core.timeseries.TimeSeriesConstants.ATTRIBUTES_PREFIX;
import static step.core.timeseries.TimeSeriesConstants.TIMESTAMP_ATTRIBUTE;
import static step.core.metrics.StepMetricSample.METRIC_TYPE;
import static step.core.metrics.MetricsExecutionPlugin.ATTRIBUTE_EXECUTION_ID;
import static step.core.metrics.MetricsConstants.*;

@Plugin(dependencies = {MigrationManagerPlugin.class, AsyncTaskManagerPlugin.class, MetricsControllerPlugin.class})
public class TimeSeriesControllerPlugin extends AbstractControllerPlugin {

    private static final Logger logger = LoggerFactory.getLogger(TimeSeriesControllerPlugin.class);
    public static final String TIME_SERIES_MAIN_COLLECTION = "timeseries";
    // Starting with Step 30, timeseries attributes are open and only a subset of known attributes are excluded (agent url, rnId).
    // The list of excluded attributes can be customized via step.properties
    // If a query filter or groupBy use an excluded attribute we fall back to RAW measurements
    public static final String TIME_SERIES_EXCLUDED_ATTRIBUTES_PROPERTY = "timeseries.attributes.excluded";
    public static final String TIME_SERIES_EXCLUDED_ATTRIBUTES_DEFAULT = AGENT_URL + "," + RN_ID;
    // Before Step 30, the list of supported attributed by the time-series were defined with below default values and could be customized via step.properties
    // This was used to determine if we had to fall back to RAW measurement when a filter or group by used unknown fields
    public static final String TIME_SERIES_ATTRIBUTES_PROPERTY = "timeseries.attributes";
    public static final String TIME_SERIES_ATTRIBUTES_DEFAULT = MetricsConstants.getAllAttributeNames() + ",metricType,origin,project";

    // Following properties are used by the UI. In the future we could remove the prefix 'plugins.' to align with other properties
    public static final String PARAM_KEY_EXECUTION_DASHBOARD_ID = "plugins.timeseries.execution.dashboard.id";
    public static final String PARAM_KEY_ANALYTICS_DASHBOARD_ID = "plugins.timeseries.analytics.dashboard.id";
    public static final String PARAM_KEY_RESPONSE_IDEAL_INTERVALS = "timeseries.response.intervals.ideal";
    public static final String PARAM_KEY_RESPONSE_MAX_INTERVALS = "timeseries.response.intervals.max";

    public static final String EXECUTION_DASHBOARD_PREPOPULATED_NAME = "Execution Dashboard";
    public static final String ANALYTICS_DASHBOARD_PREPOPULATED_NAME = "Analytics Dashboard";
    public static final String GRID_MONITORING_DASHBOARD_PREPOPULATED_NAME = "Grid Monitoring";
    public static final String EXECUTIONS_OVERVIEW_DASHBOARD_PREPOPULATED_NAME = "Executions Overview";
    public static final String GENERATION_NAME = "generationName";

    private DashboardAccessor dashboardAccessor;
    private TimeSeries timeSeries;

    @Override
    public void serverStart(GlobalContext context) {
        Configuration configuration = context.getConfiguration();
        // Either use the included attributes mode in which case only the configured attributes are ingested (this is not the default mode)
        Set<String> includedAttributes = Arrays.stream(configuration.getProperty(TIME_SERIES_ATTRIBUTES_PROPERTY, "").split(","))
            .filter(s -> !s.isEmpty())
            .map(String::trim)
            .collect(Collectors.toCollection(HashSet::new));
        // Or use the excluded attributes mode in which case all attributes except the ones explicitly excluded are ingested (default mode)
        Set<String> excludedAttributes = Arrays.stream(configuration.getProperty(TIME_SERIES_EXCLUDED_ATTRIBUTES_PROPERTY, TIME_SERIES_EXCLUDED_ATTRIBUTES_DEFAULT).split(","))
            .filter(s -> !s.isEmpty())
            .map(String::trim)
            .collect(Collectors.toCollection(HashSet::new));

        if (!includedAttributes.isEmpty() && !excludedAttributes.isEmpty()) {
            //Only one mode can be used
            throw new PluginCriticalException("Setting both the properties " + TIME_SERIES_ATTRIBUTES_PROPERTY + " and " + TIME_SERIES_EXCLUDED_ATTRIBUTES_PROPERTY + " is not allowed.");
        }

        // Following set of attributes must always be excluded from the ingestion for both modes,
        // so we add them to the list of attributes to be excluded in exclude modes or remove them in includes mode
        if (includedAttributes.isEmpty()) {
            excludedAttributes.add(BEGIN);
            excludedAttributes.add(VALUE);
        } else {
            includedAttributes.remove(VALUE);
            includedAttributes.remove(BEGIN);
        }

        MigrationManager migrationManager = context.require(MigrationManager.class);
        migrationManager.register(MigrateDashboardsTask.class);
        migrationManager.register(MigrateAggregateTask.class);
        migrationManager.register(MigrateResolutionsWithIgnoredFieldsTask.class);

        CollectionFactory collectionFactory = context.getCollectionFactory();

        TimeSeriesConfig timeSeriesConfig = TimeSeriesConfig.fromConfiguration(configuration, TIME_SERIES_MAIN_COLLECTION);
        timeSeries = new TimeSeriesBuilder()
            .withConfig(timeSeriesConfig, collectionFactory, TIME_SERIES_MAIN_COLLECTION, Set.of(ATTRIBUTE_EXECUTION_ID))
            .withAggregationConfig(new TimeSeriesAggregationConfig()
                .setIdealResponseIntervals(configuration.getPropertyAsInteger(PARAM_KEY_RESPONSE_IDEAL_INTERVALS, TimeSeriesAggregationConfig.DEFAULT_IDEAL_RESPONSE_INTERVALS))
                .setResponseMaxIntervals(configuration.getPropertyAsInteger(PARAM_KEY_RESPONSE_MAX_INTERVALS, TimeSeriesAggregationConfig.DEFAULT_RESPONSE_MAX_INTERVALS))
            )
            .build();
        TimeSeriesIngestionPipeline mainIngestionPipeline = timeSeries.getIngestionPipeline();

        TimeSeriesAggregationPipeline aggregationPipeline = timeSeries.getAggregationPipeline();
        // Safeguard against high cardinality on user-defined metric/measurement labels during executions.
        int maxUniqueLabelValues = configuration.getPropertyAsInteger("timeseries.attributes.max-unique-label-values", 20);
        ExecutionNoticeManager executionNoticeManager = context.require(ExecutionNoticeManager.class);
        // The documentation URL is keyed on the Step "doc" version (the minor component, e.g. "30" for 3.30.0),
        // derived from the version constant so it tracks version upgrades automatically.
        String docUrl = "https://step.dev/knowledgebase/" + Constants.STEP_VERSION.getMinor()
            + "/userdocs/analytics/measurements-and-metrics/#label-cardinality-safeguard";
        executionNoticeManager.register(new ExecutionNoticeType(
            TimeSeriesMetricSamplesHandler.CARDINALITY_NOTICE_TYPE_ID,
            "Time-series",
            ExecutionNoticeSeverity.WARNING,
            "High cardinality detected on the custom metric label <b>{labelName}</b> of metric <b>{metricName}</b>. " +
                "Unique values exceeding the quota of {quota} were dismissed and are reported under a single placeholder value. " +
                "<a href=\"" + docUrl + "\" target=\"_blank\">Learn more</a>"));
        TimeSeriesMetricSamplesHandler handler = new TimeSeriesMetricSamplesHandler(timeSeries, includedAttributes, excludedAttributes, maxUniqueLabelValues, executionNoticeManager);

        context.put(TimeSeries.class, timeSeries);
        context.put(TimeSeriesIngestionPipeline.class, mainIngestionPipeline);
        context.put(TimeSeriesAggregationPipeline.class, aggregationPipeline);

        context.put(TimeSeriesMetricSamplesHandler.class, handler);
        context.getServiceRegistrationCallback().registerService(TimeSeriesService.class);

        // dashboards
        Collection<DashboardView> dashboardsCollection = context.getCollectionFactory().getCollection(EntityConstants.dashboards, DashboardView.class);
        dashboardAccessor = new DashboardAccessor(dashboardsCollection);
        context.getEntityManager().register(new Entity<>(EntityConstants.dashboards, dashboardAccessor, DashboardView.class));
        context.put(DashboardAccessor.class, dashboardAccessor);
        context.getServiceRegistrationCallback().registerService(DashboardsService.class);

        TableRegistry tableRegistry = context.get(TableRegistry.class);
        tableRegistry.register(EntityConstants.dashboards, new Table<>(dashboardsCollection, "dashboard-read", true));

        MetricsExecutionPlugin.registerSamplesHandlers(handler);
        MetricSamplerRegistry.getInstance().registerHandler(handler);

        WebApplicationConfigurationManager configurationManager = context.require(WebApplicationConfigurationManager.class);
        // Following property is used by the UI. We could align its name with the configuration property in the future
        configurationManager.registerHook(s -> Map.of("plugins.timeseries.resolution.period", String.valueOf(timeSeries.getDefaultCollection().getResolutionMs())));
    }

    @Override
    public ExecutionEnginePlugin getExecutionEnginePlugin() {
        return new TimeSeriesExecutionPlugin(this.timeSeries);
    }

    @Override
    public void initializeData(GlobalContext context) throws Exception {
        super.initializeData(context);
        timeSeries.createIndexes(new LinkedHashSet<>(List.of(new IndexField(ATTRIBUTE_EXECUTION_ID, Order.ASC, String.class))));
        IndexField metricTypeIndexField = new IndexField(ATTRIBUTES_PREFIX + METRIC_TYPE, Order.ASC, String.class);
        IndexField beginIndexField = new IndexField(TIMESTAMP_ATTRIBUTE, Order.ASC, Long.class);
        timeSeries.createCompoundIndex(new LinkedHashSet<>(List.of(
            metricTypeIndexField,
            beginIndexField
        )));
        timeSeries.createCompoundIndex(new LinkedHashSet<>(List.of(
            new IndexField(ATTRIBUTES_PREFIX + TASK_ATTRIBUTE.getName(), Order.ASC, String.class),
            metricTypeIndexField,
            beginIndexField
        )));
        timeSeries.createCompoundIndex(new LinkedHashSet<>(List.of(
            new IndexField(ATTRIBUTES_PREFIX + PLAN_ATTRIBUTE.getName(), Order.ASC, String.class),
            metricTypeIndexField,
            beginIndexField
        )));

        DashboardView existingExecutionDashboard = dashboardAccessor.findByCriteria(
            Map.of(
                "attributes.name", EXECUTION_DASHBOARD_PREPOPULATED_NAME,
                "customFields." + GENERATION_NAME, EXECUTION_DASHBOARD_PREPOPULATED_NAME)
        );
        DashboardView existingAnalyticsDashboard = dashboardAccessor.findByCriteria(
            Map.of("attributes.name", ANALYTICS_DASHBOARD_PREPOPULATED_NAME,
                "customFields." + GENERATION_NAME, ANALYTICS_DASHBOARD_PREPOPULATED_NAME));
        DashboardView existingGridMonitoringDashboard = dashboardAccessor.findByCriteria(
            Map.of("attributes.name", GRID_MONITORING_DASHBOARD_PREPOPULATED_NAME,
                "customFields." + GENERATION_NAME, GRID_MONITORING_DASHBOARD_PREPOPULATED_NAME));
        DashboardView existingExecutionsOverviewDashboard = dashboardAccessor.findByCriteria(
            Map.of("attributes.name", EXECUTIONS_OVERVIEW_DASHBOARD_PREPOPULATED_NAME,
                "customFields." + GENERATION_NAME, EXECUTIONS_OVERVIEW_DASHBOARD_PREPOPULATED_NAME));

        MetricTypeRegistry metricTypeRegistry = context.require(MetricTypeRegistry.class);
        DashboardsGenerator dashboardsGenerator = new DashboardsGenerator(metricTypeRegistry.getMetrics());
        DashboardView newExecutionDashboard = dashboardsGenerator.createExecutionDashboard();
        DashboardView newAnalyticsDashboard = dashboardsGenerator.createAnalyticsDashboard();
        DashboardView newGridMonitoringDashboard = dashboardsGenerator.createGridMonitoringDashboard();
        DashboardView newExecutionsOverviewDashboard = dashboardsGenerator.createExecutionsOverviewDashboard();
        if (existingExecutionDashboard != null) {
            newExecutionDashboard.setId(existingExecutionDashboard.getId());
        }
        if (existingAnalyticsDashboard != null) {
            newAnalyticsDashboard.setId(existingAnalyticsDashboard.getId());
        }
        if (existingGridMonitoringDashboard != null) {
            newGridMonitoringDashboard.setId(existingGridMonitoringDashboard.getId());
        }
        if (existingExecutionsOverviewDashboard != null) {
            newExecutionsOverviewDashboard.setId(existingExecutionsOverviewDashboard.getId());
        }
        dashboardAccessor.save(newExecutionDashboard);
        dashboardAccessor.save(newAnalyticsDashboard);
        dashboardAccessor.save(newGridMonitoringDashboard);
        dashboardAccessor.save(newExecutionsOverviewDashboard);

        WebApplicationConfigurationManager configurationManager = context.require(WebApplicationConfigurationManager.class);
        configurationManager.registerHook(s -> Map.of(PARAM_KEY_EXECUTION_DASHBOARD_ID, newExecutionDashboard.getId().toString()));
        configurationManager.registerHook(s -> Map.of(PARAM_KEY_ANALYTICS_DASHBOARD_ID, newAnalyticsDashboard.getId().toString()));
        AsyncTaskManager asyncTaskManager = context.require(AsyncTaskManager.class);
        initTimeSeriesCollectionsData(asyncTaskManager);
    }

    private void initTimeSeriesCollectionsData(AsyncTaskManager asyncTaskManager) {
        asyncTaskManager.scheduleAsyncTask((empty) -> {
            logger.info("TimeSeries ingestion for empty resolutions has started");
            timeSeries.ingestDataForEmptyCollections();
            logger.info("TimeSeries ingestion for empty resolutions has finished");
            return null;
        });
    }


    @Override
    public void serverStop(GlobalContext context) {
        if (timeSeries != null) {
            timeSeries.close();
        }
    }
}
