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
import step.core.entities.EntityManager;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.timeseries.*;
import step.core.timeseries.aggregation.TimeSeriesAggregationPipeline;
import step.core.timeseries.ingestion.TimeSeriesIngestionPipeline;
import step.core.timeseries.metric.*;
import step.engine.plugins.ExecutionEnginePlugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.migration.MigrationManager;
import step.migration.MigrationManagerPlugin;
import step.plugins.measurements.GaugeCollectorRegistry;
import step.plugins.measurements.MeasurementPlugin;
import step.plugins.timeseries.dashboards.DashboardAccessor;
import step.plugins.timeseries.dashboards.DashboardsGenerator;
import step.plugins.timeseries.dashboards.model.DashboardView;
import step.plugins.timeseries.migration.MigrateAggregateTask;
import step.plugins.timeseries.migration.MigrateDashboardsTask;
import step.plugins.timeseries.migration.MigrateResolutionsWithIgnoredFieldsTask;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static step.plugins.timeseries.MetricsConstants.*;
import static step.plugins.timeseries.TimeSeriesExecutionPlugin.*;

@Plugin(dependencies = {MigrationManagerPlugin.class, AsyncTaskManagerPlugin.class})
public class TimeSeriesControllerPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(TimeSeriesControllerPlugin.class);
	public static final String TIME_SERIES_MAIN_COLLECTION = "timeseries";
	public static final String TIME_SERIES_ATTRIBUTES_PROPERTY = "timeseries.attributes";
	public static final String TIME_SERIES_ATTRIBUTES_DEFAULT = EXECUTION_ID + "," + TASK_ID + "," + PLAN_ID + ",metricType,origin,name,rnStatus,project,type";

	// Following properties are used by the UI. In the future we could remove the prefix 'plugins.' to align with other properties
	public static final String PARAM_KEY_EXECUTION_DASHBOARD_ID = "plugins.timeseries.execution.dashboard.id";
	public static final String PARAM_KEY_ANALYTICS_DASHBOARD_ID = "plugins.timeseries.analytics.dashboard.id";
	public static final String PARAM_KEY_RESPONSE_IDEAL_INTERVALS = "timeseries.response.intervals.ideal";
	public static final String PARAM_KEY_RESPONSE_MAX_INTERVALS = "timeseries.response.intervals.max";

	public static final String EXECUTION_DASHBOARD_PREPOPULATED_NAME = "Execution Dashboard";
	public static final String ANALYTICS_DASHBOARD_PREPOPULATED_NAME = "Analytics Dashboard";
	public static final String GENERATION_NAME = "generationName";

	private TimeSeriesIngestionPipeline mainIngestionPipeline;
	private DashboardAccessor dashboardAccessor;
	private TimeSeries timeSeries;
	
	@Override
	public void serverStart(GlobalContext context) {
		MigrationManager migrationManager = context.require(MigrationManager.class);
		migrationManager.register(MigrateDashboardsTask.class);
		migrationManager.register(MigrateAggregateTask.class);
		migrationManager.register(MigrateResolutionsWithIgnoredFieldsTask.class);
		
		Configuration configuration = context.getConfiguration();
		List<String> attributes = Arrays.asList(configuration.getProperty(TIME_SERIES_ATTRIBUTES_PROPERTY, TIME_SERIES_ATTRIBUTES_DEFAULT).split(","));
		CollectionFactory collectionFactory = context.getCollectionFactory();

		TimeSeriesCollectionsSettings timeSeriesCollectionsSettings = TimeSeriesCollectionsSettings.readSettings(configuration, TIME_SERIES_MAIN_COLLECTION);

		TimeSeriesCollectionsBuilder timeSeriesCollectionsBuilder = new TimeSeriesCollectionsBuilder(collectionFactory);
		List<TimeSeriesCollection> enabledCollections = timeSeriesCollectionsBuilder.getTimeSeriesCollections(TIME_SERIES_MAIN_COLLECTION, timeSeriesCollectionsSettings);
		// timeseries will have a list of registered collection.
		timeSeries = new TimeSeriesBuilder()
				.setSettings(new TimeSeriesSettings()
						.setIdealResponseIntervals(configuration.getPropertyAsInteger(PARAM_KEY_RESPONSE_IDEAL_INTERVALS, TimeSeriesSettings.DEFAULT_IDEAL_RESPONSE_INTERVALS))
						.setResponseMaxIntervals(configuration.getPropertyAsInteger(PARAM_KEY_RESPONSE_MAX_INTERVALS, TimeSeriesSettings.DEFAULT_RESPONSE_MAX_INTERVALS))
				)
				.registerCollections(enabledCollections).build();
		mainIngestionPipeline = timeSeries.getIngestionPipeline();

		TimeSeriesAggregationPipeline aggregationPipeline = timeSeries.getAggregationPipeline();
		MetricTypeAccessor metricTypeAccessor = new MetricTypeAccessor(context.getCollectionFactory().getCollection(EntityManager.metricTypes, MetricType.class));
		TimeSeriesBucketingHandler handler = new TimeSeriesBucketingHandler(timeSeries, attributes);

		context.put(TimeSeries.class, timeSeries);
		context.put(TimeSeriesIngestionPipeline.class, mainIngestionPipeline);
		context.put(TimeSeriesAggregationPipeline.class, aggregationPipeline);
		context.put(MetricTypeAccessor.class, metricTypeAccessor);
		context.put(TimeSeriesBucketingHandler.class, handler);
		context.getServiceRegistrationCallback().registerService(TimeSeriesService.class);

		// dashboards
		Collection<DashboardView> dashboardsCollection = context.getCollectionFactory().getCollection(EntityManager.dashboards, DashboardView.class);
		dashboardAccessor = new DashboardAccessor(dashboardsCollection);
		context.getEntityManager().register(new Entity<>(EntityManager.dashboards, dashboardAccessor, DashboardView.class));
		context.put(DashboardAccessor.class, dashboardAccessor);
		context.getServiceRegistrationCallback().registerService(DashboardsService.class);

		TableRegistry tableRegistry = context.get(TableRegistry.class);
		tableRegistry.register(EntityManager.dashboards, new Table<>(dashboardsCollection, "dashboard-read", true));

		MeasurementPlugin.registerMeasurementHandlers(handler);
		GaugeCollectorRegistry.getInstance().registerHandler(handler);

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
		timeSeries.createIndexes(new LinkedHashSet<>(List.of(new IndexField("eId", Order.ASC, String.class))));
		List<MetricType> metrics = createOrUpdateMetrics(context.require(MetricTypeAccessor.class));

		DashboardView existingExecutionDashboard = dashboardAccessor.findByCriteria(
				Map.of(
						"attributes.name", EXECUTION_DASHBOARD_PREPOPULATED_NAME,
						"customFields." + GENERATION_NAME, EXECUTION_DASHBOARD_PREPOPULATED_NAME)
		);
		DashboardView existingAnalyticsDashboard = dashboardAccessor.findByCriteria(
				Map.of("attributes.name", ANALYTICS_DASHBOARD_PREPOPULATED_NAME,
						"customFields." + GENERATION_NAME, ANALYTICS_DASHBOARD_PREPOPULATED_NAME));

		DashboardsGenerator dashboardsGenerator = new DashboardsGenerator(metrics);
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

	private void initTimeSeriesCollectionsData(AsyncTaskManager asyncTaskManager) {
		asyncTaskManager.scheduleAsyncTask((empty) -> {
			logger.info("TimeSeries ingestion for empty resolutions has started");
			timeSeries.ingestDataForEmptyCollections();
			logger.info("TimeSeries ingestion for empty resolutions has finished");
			return null;
		});
	}
	
	
	private List<MetricType> createOrUpdateMetrics(MetricTypeAccessor metricTypeAccessor) {
		// TODO create a builder for units
		// TODO metrics shouldn't be defined centrally but in each plugin they belong to. Implement a central registration service
		List<MetricType> metrics = Arrays.asList(
				new MetricType()
						.setName(EXECUTIONS_COUNT)
						.setDisplayName("Execution count")
						.setAttributes(Arrays.asList(TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
						.setDefaultAggregation(new MetricAggregation(MetricAggregationType.SUM))
						.setUnit("1")
						.setRenderingSettings(new MetricRenderingSettings()
						),
				new MetricType()
						.setName(EXECUTIONS_DURATION)
						.setDisplayName("Execution duration")
						.setAttributes(Arrays.asList(TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE, EXECUTION_BOOLEAN_RESULT, EXECUTION_RESULT))
						.setDefaultAggregation(new MetricAggregation(MetricAggregationType.AVG))
						.setUnit("ms")
						.setRenderingSettings(new MetricRenderingSettings()),
				new MetricType()
						// AVG calculation is enough here. the value is either 0 or 100 for each exec.
						.setName(FAILURE_PERCENTAGE)
						.setDisplayName("Execution failure percentage")
						.setAttributes(Arrays.asList(TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
						.setUnit("%")
						.setDefaultAggregation(new MetricAggregation(MetricAggregationType.AVG))
						.setRenderingSettings(new MetricRenderingSettings()),
				new MetricType()
						.setName(FAILURE_COUNT)
						.setUnit("1")
						.setDisplayName("Execution failure count")
						.setAttributes(Arrays.asList(TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
						.setDefaultAggregation(new MetricAggregation(MetricAggregationType.SUM))
						.setRenderingSettings(new MetricRenderingSettings()),
				new MetricType()
						.setName(FAILURES_COUNT_BY_ERROR_CODE)
						.setDisplayName("Execution failure count by error code")
						.setUnit("1")
						.setDefaultGroupingAttributes(Arrays.asList(ERROR_CODE_ATTRIBUTE.getName()))
						.setDefaultAggregation(new MetricAggregation(MetricAggregationType.SUM))
						.setAttributes(Arrays.asList(TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE, ERROR_CODE_ATTRIBUTE))
						.setRenderingSettings(new MetricRenderingSettings()),
				new MetricType()
						.setName(RESPONSE_TIME)
						.setDisplayName("Response time")
						.setAttributes(Arrays.asList(STATUS_ATTRIBUTE, TYPE_ATRIBUTE, NAME_ATTRIBUTE, TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
						.setDefaultGroupingAttributes(Arrays.asList(NAME_ATTRIBUTE.getName()))
						.setUnit("ms")
						.setDefaultAggregation(new MetricAggregation(MetricAggregationType.AVG))
						.setRenderingSettings(new MetricRenderingSettings()),
				new MetricType()
						.setName(THREAD_GROUP)
						.setDisplayName("Thread group")
						.setAttributes(Arrays.asList(TYPE_ATRIBUTE, NAME_ATTRIBUTE, TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
						.setDefaultGroupingAttributes(Arrays.asList(NAME_ATTRIBUTE.getName()))
						.setUnit("1")
						.setDefaultAggregation(new MetricAggregation(MetricAggregationType.MAX))
						.setRenderingSettings(new MetricRenderingSettings())
		);
		metrics.forEach(m -> {
			MetricType existingMetric = metricTypeAccessor.findByCriteria(Map.of("name", m.getName()));
			if (existingMetric != null) {
				m.setId(existingMetric.getId()); // update the metric
			}
			metricTypeAccessor.save(m);
		});
		return metrics;
	}
	
	@Override
	public void serverStop(GlobalContext context) {
		if (timeSeries != null) {
			timeSeries.close();
		}
	}
}
