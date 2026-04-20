package step.plugins.timeseries;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.controller.services.async.AsyncTaskManager;
import step.controller.services.async.AsyncTaskManagerPlugin;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.IndexField;
import step.core.collections.Order;
import step.core.deployment.WebApplicationConfigurationManager;
import step.core.entities.Entity;
import step.core.entities.EntityConstants;
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
import step.plugins.metrics.MetricSamplerRegistry;
import step.plugins.metrics.MetricTypeRegistry;
import step.plugins.metrics.SamplesControllerPlugin;
import step.plugins.metrics.SamplesExecutionPlugin;
import step.plugins.timeseries.dashboards.DashboardAccessor;
import step.plugins.timeseries.dashboards.DashboardsGenerator;
import step.plugins.timeseries.dashboards.model.DashboardView;
import step.plugins.timeseries.migration.MigrateAggregateTask;
import step.plugins.timeseries.migration.MigrateDashboardsTask;
import step.plugins.timeseries.migration.MigrateResolutionsWithIgnoredFieldsTask;

import java.util.*;
import java.util.stream.Collectors;

import static step.core.timeseries.TimeSeriesConstants.ATTRIBUTES_PREFIX;
import static step.core.timeseries.TimeSeriesConstants.TIMESTAMP_ATTRIBUTE;
import static step.plugins.metrics.AbstractMetricSample.METRIC_TYPE;
import static step.plugins.metrics.SamplesExecutionPlugin.ATTRIBUTE_EXECUTION_ID;
import static step.plugins.metrics.MetricsConstants.*;

@Plugin(dependencies = {MigrationManagerPlugin.class, AsyncTaskManagerPlugin.class, SamplesControllerPlugin.class})
public class TimeSeriesControllerPlugin extends AbstractControllerPlugin {

    private static final Logger logger = LoggerFactory.getLogger(TimeSeriesControllerPlugin.class);
    public static final String TIME_SERIES_MAIN_COLLECTION = "timeseries";
    // Starting with Step 30, timeseries attributes are open and only a subset of known attributes are excluded (agent url, rnId).
    // The list of excluded attributes can be customized via step.properties
    // If a query filter or groupBy use an excluded attribute we fall back to RAW measurements
    public static final String TIME_SERIES_EXCLUDED_ATTRIBUTES_PROPERTY = "timeseries.attributes.excluded";
    public static final String TIME_SERIES_EXCLUDED_ATTRIBUTES_DEFAULT = "agentUrl,rnId";
    // Before Step 30, the list of supported attributed by the time-series were defined with below default values and could be customized via step.properties
    // This was used to determine if we had to fall back to RAW measurement when a filter or group by used unknown fields
    public static final String TIME_SERIES_ATTRIBUTES_PROPERTY = "timeseries.attributes";
    public static final String TIME_SERIES_ATTRIBUTES_DEFAULT = step.plugins.metrics.MetricsConstants.getAllAttributeNames() + ",metricType,origin,project";

    // Following properties are used by the UI. In the future we could remove the prefix 'plugins.' to align with other properties
    public static final String PARAM_KEY_EXECUTION_DASHBOARD_ID = "plugins.timeseries.execution.dashboard.id";
    public static final String PARAM_KEY_ANALYTICS_DASHBOARD_ID = "plugins.timeseries.analytics.dashboard.id";
    public static final String PARAM_KEY_RESPONSE_IDEAL_INTERVALS = "timeseries.response.intervals.ideal";
    public static final String PARAM_KEY_RESPONSE_MAX_INTERVALS = "timeseries.response.intervals.max";

    public static final String EXECUTION_DASHBOARD_PREPOPULATED_NAME = "Execution Dashboard";
    public static final String ANALYTICS_DASHBOARD_PREPOPULATED_NAME = "Analytics Dashboard";
    public static final String GENERATION_NAME = "generationName";

    private DashboardAccessor dashboardAccessor;
    private TimeSeries timeSeries;

    @Override
    public void serverStart(GlobalContext context) {
        Configuration configuration = context.getConfiguration();
        Set<String> includedAttributes = Arrays.stream(configuration.getProperty(TIME_SERIES_ATTRIBUTES_PROPERTY, "").split(","))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        Set<String> excludedAttributes = Arrays.stream(configuration.getProperty(TIME_SERIES_EXCLUDED_ATTRIBUTES_PROPERTY, TIME_SERIES_EXCLUDED_ATTRIBUTES_DEFAULT).split(","))
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());

        if (!includedAttributes.isEmpty() && !excludedAttributes.isEmpty()) {
            throw new PluginCriticalException("Setting both the properties " + TIME_SERIES_ATTRIBUTES_PROPERTY + " and "  + TIME_SERIES_EXCLUDED_ATTRIBUTES_PROPERTY + " is not allowed.");
        }

        MigrationManager migrationManager = context.require(MigrationManager.class);
        migrationManager.register(MigrateDashboardsTask.class);
        migrationManager.register(MigrateAggregateTask.class);
        migrationManager.register(MigrateResolutionsWithIgnoredFieldsTask.class);

        CollectionFactory collectionFactory = context.getCollectionFactory();

        TimeSeriesConfig timeSeriesConfig = TimeSeriesConfig.fromConfiguration(configuration, TIME_SERIES_MAIN_COLLECTION);
        warnAboutDisabledCollections(timeSeriesConfig);
        timeSeries = new TimeSeriesBuilder()
            .withConfig(timeSeriesConfig, collectionFactory, TIME_SERIES_MAIN_COLLECTION, Set.of(ATTRIBUTE_EXECUTION_ID))
            .setAggregationConfig(new TimeSeriesAggregationConfig()
                .setIdealResponseIntervals(configuration.getPropertyAsInteger(PARAM_KEY_RESPONSE_IDEAL_INTERVALS, TimeSeriesAggregationConfig.DEFAULT_IDEAL_RESPONSE_INTERVALS))
                .setResponseMaxIntervals(configuration.getPropertyAsInteger(PARAM_KEY_RESPONSE_MAX_INTERVALS, TimeSeriesAggregationConfig.DEFAULT_RESPONSE_MAX_INTERVALS))
            )
            .build();
        TimeSeriesIngestionPipeline mainIngestionPipeline = timeSeries.getIngestionPipeline();

        TimeSeriesAggregationPipeline aggregationPipeline = timeSeries.getAggregationPipeline();
        TimeSeriesMetricSamplesHandler handler = new TimeSeriesMetricSamplesHandler(timeSeries, includedAttributes, excludedAttributes);

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

        SamplesExecutionPlugin.registerSamplesHandlers(handler);
        MetricSamplerRegistry.getInstance().registerHandler(handler);

        WebApplicationConfigurationManager configurationManager = context.require(WebApplicationConfigurationManager.class);
        // Following property is used by the UI. We could align its name with the configuration property in the future
        configurationManager.registerHook(s -> Map.of("plugins.timeseries.resolution.period", String.valueOf(timeSeries.getDefaultCollection().getResolution())));
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

        MetricTypeRegistry metricTypeRegistry = context.require(MetricTypeRegistry.class);
        DashboardsGenerator dashboardsGenerator = new DashboardsGenerator(metricTypeRegistry.getMetrics());
        DashboardView newExecutionDashboard = dashboardsGenerator.createExecutionDashboard();
        DashboardView newAnalyticsDashboard = dashboardsGenerator.createAnalyticsDashboard();
        if (existingExecutionDashboard != null) {
            newExecutionDashboard.setId(existingExecutionDashboard.getId());
        }
        if (existingAnalyticsDashboard != null) {
            newAnalyticsDashboard.setId(existingAnalyticsDashboard.getId());
        }
        dashboardAccessor.save(newExecutionDashboard);
        dashboardAccessor.save(newAnalyticsDashboard);

        WebApplicationConfigurationManager configurationManager = context.require(WebApplicationConfigurationManager.class);
        configurationManager.registerHook(s -> Map.of(PARAM_KEY_EXECUTION_DASHBOARD_ID, newExecutionDashboard.getId().toString()));
        configurationManager.registerHook(s -> Map.of(PARAM_KEY_ANALYTICS_DASHBOARD_ID, newAnalyticsDashboard.getId().toString()));
        AsyncTaskManager asyncTaskManager = context.require(AsyncTaskManager.class);
        initTimeSeriesCollectionsData(asyncTaskManager);
    }

    private void warnAboutDisabledCollections(TimeSeriesConfig config) {
        if (!config.isPerMinuteEnabled()) {
            logger.warn("The time-series resolution '_minute' is disabled. To reclaim space you can delete the corresponding DB table.");
        }
        if (!config.isHourlyEnabled()) {
            logger.warn("The time-series resolution '_hour' is disabled. To reclaim space you can delete the corresponding DB table.");
        }
        if (!config.isDailyEnabled()) {
            logger.warn("The time-series resolution '_day' is disabled. To reclaim space you can delete the corresponding DB table.");
        }
        if (!config.isWeeklyEnabled()) {
            logger.warn("The time-series resolution '_week' is disabled. To reclaim space you can delete the corresponding DB table.");
        }
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
