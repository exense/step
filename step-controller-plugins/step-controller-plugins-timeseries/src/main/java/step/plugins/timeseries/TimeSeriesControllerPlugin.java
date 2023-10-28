package step.plugins.timeseries;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.collections.CollectionFactory;
import step.core.deployment.WebApplicationConfigurationManager;
import step.core.entities.EntityManager;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.TimeSeriesIngestionPipeline;
import step.core.timeseries.aggregation.TimeSeriesAggregationPipeline;
import step.core.timeseries.metric.*;
import step.engine.plugins.ExecutionEnginePlugin;
import step.plugins.measurements.GaugeCollectorRegistry;
import step.plugins.measurements.MeasurementPlugin;

import static step.plugins.timeseries.TimeSeriesExecutionPlugin.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Plugin
public class TimeSeriesControllerPlugin extends AbstractControllerPlugin {

    public static String RESOLUTION_PERIOD_PROPERTY = "plugins.timeseries.resolution.period";
    public static String TIME_SERIES_SAMPLING_LIMIT = "plugins.timeseries.sampling.limit";
    public static String TIME_SERIES_COLLECTION_PROPERTY = "timeseries";

    public static String TIME_SERIES_ATTRIBUTES_PROPERTY = "plugins.timeseries.attributes";
    public static String TIME_SERIES_ATTRIBUTES_DEFAULT = EXECUTION_ID + "," + TASK_ID + "," + PLAN_ID + ",metricType,origin,name,rnStatus,project,type";

    private static final Logger logger = LoggerFactory.getLogger(TimeSeriesControllerPlugin.class);
    private TimeSeriesIngestionPipeline mainIngestionPipeline;
    private TimeSeriesAggregationPipeline aggregationPipeline;

    @Override
    public void serverStart(GlobalContext context) {
        Configuration configuration = context.getConfiguration();
        Integer resolutionPeriod = configuration.getPropertyAsInteger(RESOLUTION_PERIOD_PROPERTY, 1000);
        Long flushPeriod = configuration.getPropertyAsLong("plugins.timeseries.flush.period", 1000L);
        List<String> attributes = Arrays.asList(configuration.getProperty(TIME_SERIES_ATTRIBUTES_PROPERTY, TIME_SERIES_ATTRIBUTES_DEFAULT).split(","));
        CollectionFactory collectionFactory = context.getCollectionFactory();

        TimeSeries timeSeries = new TimeSeries(collectionFactory, TIME_SERIES_COLLECTION_PROPERTY, Set.of(), resolutionPeriod);
        context.put(TimeSeries.class, timeSeries);
        mainIngestionPipeline = timeSeries.newIngestionPipeline(flushPeriod);
        aggregationPipeline = timeSeries.getAggregationPipeline();
        TimeSeriesAggregationPipeline aggregationPipeline = timeSeries.getAggregationPipeline();
        MetricTypeAccessor metricTypeAccessor = new MetricTypeAccessor(context.getCollectionFactory().getCollection(EntityManager.metricTypes, MetricType.class));

        context.put(TimeSeriesIngestionPipeline.class, mainIngestionPipeline);
        context.put(TimeSeriesAggregationPipeline.class, aggregationPipeline);
        context.put(MetricTypeAccessor.class, metricTypeAccessor);

        context.getServiceRegistrationCallback().registerService(TimeSeriesService.class);
        TimeSeriesBucketingHandler handler = new TimeSeriesBucketingHandler(mainIngestionPipeline, attributes);
        context.put(TimeSeriesBucketingHandler.class, handler);
        MeasurementPlugin.registerMeasurementHandlers(handler);
        GaugeCollectorRegistry.getInstance().registerHandler(handler);

        WebApplicationConfigurationManager configurationManager = context.require(WebApplicationConfigurationManager.class);
        configurationManager.registerHook(s -> Map.of(RESOLUTION_PERIOD_PROPERTY, resolutionPeriod.toString()));

    }

    @Override
    public ExecutionEnginePlugin getExecutionEnginePlugin() {
        return new TimeSeriesExecutionPlugin(mainIngestionPipeline, aggregationPipeline);
    }

    @Override
    public void initializeData(GlobalContext context) {
        MetricAttribute taskAttribute = new MetricAttribute().setName("taskId").setDisplayName("Task");
        MetricAttribute executionAttribute = new MetricAttribute().setName("eId").setDisplayName("Execution");
        MetricAttribute planAttribute = new MetricAttribute().setName("planId").setDisplayName("Plan");
        MetricAttribute nameAttribute = new MetricAttribute().setName("name").setDisplayName("Name");
        MetricAttribute errorCodeAttribute = new MetricAttribute().setName("errorCode").setDisplayName("Error Code");

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
                        .setDisplayName("Execution failure count")
                        .setAttributes(Arrays.asList(taskAttribute, executionAttribute, planAttribute))
                        .setDefaultAggregation(MetricAggregation.COUNT)
                        .setRenderingSettings(new MetricRenderingSettings()),
                new MetricType()
                        .setName(FAILURES_COUNT_BY_ERROR_CODE)
                        .setDisplayName("Execution failure count by error code")
                        .setUnit("1")
                        .setDefaultGroupingAttributes(Arrays.asList(errorCodeAttribute.getName()))
                        .setDefaultAggregation(MetricAggregation.RATE)
                        .setAttributes(Arrays.asList(taskAttribute, executionAttribute, planAttribute, errorCodeAttribute))
                        .setRenderingSettings(new MetricRenderingSettings()),

                new MetricType()
                        .setName("response-time")
                        .setDisplayName("Response time")
                        .setAttributes(Arrays.asList(nameAttribute, taskAttribute, executionAttribute, planAttribute))
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
