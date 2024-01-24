package step.plugins.timeseries;

import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.deployment.WebApplicationConfigurationManager;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.TimeSeriesIngestionPipeline;
import step.core.timeseries.aggregation.TimeSeriesAggregationPipeline;
import step.core.timeseries.metric.*;
import step.engine.plugins.ExecutionEnginePlugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.plugins.measurements.GaugeCollectorRegistry;
import step.plugins.measurements.MeasurementPlugin;
import step.plugins.timeseries.dashboards.model.*;
import step.plugins.timeseries.dashboards.DashboardAccessor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static step.plugins.timeseries.TimeSeriesExecutionPlugin.*;

@Plugin
public class TimeSeriesControllerPlugin extends AbstractControllerPlugin {

	public static final String PLUGINS_TIMESERIES_FLUSH_PERIOD = "plugins.timeseries.flush.period";
	public static final String RESOLUTION_PERIOD_PROPERTY = "plugins.timeseries.resolution.period";
	public static final String TIME_SERIES_SAMPLING_LIMIT = "plugins.timeseries.sampling.limit";
	public static final String TIME_SERIES_COLLECTION_PROPERTY = "timeseries";
	public static final String TIME_SERIES_ATTRIBUTES_PROPERTY = "plugins.timeseries.attributes";
	public static final String TIME_SERIES_ATTRIBUTES_DEFAULT = EXECUTION_ID + "," + TASK_ID + "," + PLAN_ID + ",metricType,origin,name,rnStatus,project,type";

	private TimeSeriesIngestionPipeline mainIngestionPipeline;
	private TimeSeriesAggregationPipeline aggregationPipeline;
	private DashboardAccessor dashboardAccessor;

	@Override
	public void serverStart(GlobalContext context) {
		Configuration configuration = context.getConfiguration();
		Integer resolutionPeriod = configuration.getPropertyAsInteger(RESOLUTION_PERIOD_PROPERTY, 1000);
		Long flushPeriod = configuration.getPropertyAsLong(PLUGINS_TIMESERIES_FLUSH_PERIOD, 1000L);
		List<String> attributes = Arrays.asList(configuration.getProperty(TIME_SERIES_ATTRIBUTES_PROPERTY, TIME_SERIES_ATTRIBUTES_DEFAULT).split(","));
		CollectionFactory collectionFactory = context.getCollectionFactory();

		TimeSeries timeSeries = new TimeSeries(collectionFactory, TIME_SERIES_COLLECTION_PROPERTY, Set.of("eId"), resolutionPeriod);
		mainIngestionPipeline = timeSeries.newIngestionPipeline(flushPeriod);
		aggregationPipeline = timeSeries.getAggregationPipeline();
		TimeSeriesAggregationPipeline aggregationPipeline = timeSeries.getAggregationPipeline();
		MetricTypeAccessor metricTypeAccessor = new MetricTypeAccessor(context.getCollectionFactory().getCollection(EntityManager.metricTypes, MetricType.class));
		TimeSeriesBucketingHandler handler = new TimeSeriesBucketingHandler(mainIngestionPipeline, attributes);

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
		configurationManager.registerHook(s -> Map.of(RESOLUTION_PERIOD_PROPERTY, resolutionPeriod.toString()));
	}

	@Override
	public ExecutionEnginePlugin getExecutionEnginePlugin() {
		return new TimeSeriesExecutionPlugin(mainIngestionPipeline, aggregationPipeline);
	}

	private void createLegacyDashboard() {
		boolean legacyDashboardExists = dashboardAccessor.findLegacyDashboards().findFirst().isPresent();
		if (!legacyDashboardExists) {
			DashboardView dashboard = new DashboardView();
			dashboard.setName("Performance Dashboard");
			dashboard.setDescription("Default dashboard displaying performance of all executions");
			dashboard.getMetadata().put("isLegacy", true);
			dashboard.getMetadata().put("link", "analytics");
			dashboardAccessor.save(dashboard);
		}

	}

	@Override
	public void initializeData(GlobalContext context) {
		//Create legacy dashboards
		createLegacyDashboard();

        MetricAttribute statusAttribute = new MetricAttribute()
                .setName("rnStatus")
                .setType(MetricAttributeType.TEXT)
                .setMetadata(Map.of("knownValues", Arrays.asList("PASSED", "FAILED", "TECHNICAL_ERROR", "INTERRUPTED")))
                .setDisplayName("Status");
        MetricAttribute taskAttribute = new MetricAttribute()
                .setName("taskId")
                .setType(MetricAttributeType.TEXT)
                .setMetadata(Map.of("entity", "task"))
                .setDisplayName("Task");
        MetricAttribute executionAttribute = new MetricAttribute()
                .setName("eId")
                .setType(MetricAttributeType.TEXT)
                .setMetadata(Map.of("entity", "execution"))
                .setDisplayName("Execution");
        MetricAttribute planAttribute = new MetricAttribute()
                .setName("planId")
                .setType(MetricAttributeType.TEXT)
                .setMetadata(Map.of("entity", "plan"))
                .setDisplayName("Plan");
        MetricAttribute nameAttribute = new MetricAttribute()
                .setName("name")
                .setType(MetricAttributeType.TEXT)
                .setDisplayName("Name");
        MetricAttribute errorCodeAttribute = new MetricAttribute()
                .setName("errorCode")
                .setDisplayName("Error Code");

        // TODO create a builder for units
        // TODO metrics shouldn't be defined centrally but in each plugin they belong to. Implement a central registration service
        MetricTypeAccessor metricTypeAccessor = context.require(MetricTypeAccessor.class);
        List<MetricType> metrics = Arrays.asList(
                new MetricType()
                        .setName(EXECUTIONS_COUNT)
                        .setDisplayName("Execution count")
                        .setAttributes(Arrays.asList(taskAttribute, executionAttribute, planAttribute))
                        .setDefaultAggregation(MetricAggregation.SUM)
                        .setUnit("1")
                        .setRenderingSettings(new MetricRenderingSettings()
                        ),
                new MetricType()
                        // AVG calculation is enough here. the value is either 0 or 100 for each exec.
                        .setName(FAILURE_PERCENTAGE)
                        .setDisplayName("Execution failure percentage")
                        .setAttributes(Arrays.asList(taskAttribute, executionAttribute, planAttribute))
                        .setUnit("%")
                        .setDefaultAggregation(MetricAggregation.AVG)
                        .setRenderingSettings(new MetricRenderingSettings()),
                new MetricType()
                        .setName(FAILURE_COUNT)
                        .setUnit("1")
                        .setDisplayName("Execution failure count")
                        .setAttributes(Arrays.asList(taskAttribute, executionAttribute, planAttribute))
                        .setDefaultAggregation(MetricAggregation.SUM)
                        .setRenderingSettings(new MetricRenderingSettings()),
                new MetricType()
                        .setName(FAILURES_COUNT_BY_ERROR_CODE)
                        .setDisplayName("Execution failure count by error code")
                        .setUnit("1")
                        .setDefaultGroupingAttributes(Arrays.asList(errorCodeAttribute.getName()))
                        .setDefaultAggregation(MetricAggregation.SUM)
                        .setAttributes(Arrays.asList(taskAttribute, executionAttribute, planAttribute, errorCodeAttribute))
                        .setRenderingSettings(new MetricRenderingSettings()),
                new MetricType()
                        .setName(RESPONSE_TIME)
                        .setDisplayName("Response time")
                        .setAttributes(Arrays.asList(statusAttribute, nameAttribute, taskAttribute, executionAttribute, planAttribute))
                        .setDefaultGroupingAttributes(Arrays.asList(nameAttribute.getName()))
                        .setUnit("ms")
                        .setDefaultAggregation(MetricAggregation.AVG)
                        .setRenderingSettings(new MetricRenderingSettings())
        );
        metrics.forEach(m -> {
            MetricType existingMetric = metricTypeAccessor.findByCriteria(Map.of("name", m.getName()));
            if (existingMetric != null) {
                m.setId(existingMetric.getId()); // update the metric
            }
            metricTypeAccessor.save(m);
        });
    }

	@Override
	public void serverStop(GlobalContext context) {
		mainIngestionPipeline.close();
	}
}
