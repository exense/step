package step.plugins.timeseries;

import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.collections.CollectionFactory;
import step.core.deployment.WebApplicationConfigurationManager;
import step.core.execution.ExecutionContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.TimeSeriesIngestionPipeline;
import step.core.timeseries.aggregation.TimeSeriesAggregationPipeline;
import step.core.timeseries.bucket.BucketAttributes;
import step.core.views.ViewManager;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.ExecutionEnginePlugin;
import step.plugins.measurements.GaugeCollectorRegistry;
import step.plugins.measurements.MeasurementPlugin;
import step.plugins.views.functions.ErrorDistribution;
import step.plugins.views.functions.ErrorDistributionView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static step.plugins.timeseries.TimeSeriesExecutionPlugin.*;

@Plugin
public class TimeSeriesControllerPlugin extends AbstractControllerPlugin {

    public static String RESOLUTION_PERIOD_PROPERTY = "plugins.timeseries.resolution.period";
    public static String TIME_SERIES_SAMPLING_LIMIT = "plugins.timeseries.sampling.limit";
    public static String TIME_SERIES_COLLECTION_PROPERTY = "timeseries";
    public static String TIME_SERIES_ATTRIBUTES_PROPERTY = "plugins.timeseries.attributes";
    public static String TIME_SERIES_ATTRIBUTES_DEFAULT = EXECUTION_ID + "," + TASK_ID + "," + PLAN_ID + ",metricType,origin,name,rnStatus,project,type";

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

        context.put(TimeSeriesIngestionPipeline.class, mainIngestionPipeline);
        context.put(TimeSeriesAggregationPipeline.class, aggregationPipeline);

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
        return new AbstractExecutionEnginePlugin() {
            @Override
            public void executionStart(ExecutionContext context) {
                context.put(TimeSeriesAggregationPipeline.class, aggregationPipeline);
                context.put(TimeSeriesIngestionPipeline.class, mainIngestionPipeline);
            }

            @Override
            public void afterExecutionEnd(ExecutionContext context) {
                ExecutionAccessor executionAccessor = context.getExecutionAccessor();
                Execution execution = executionAccessor.get(context.getExecutionId());
                TimeSeriesIngestionPipeline ingestionPipeline = context.require(TimeSeriesIngestionPipeline.class);
                ViewManager viewManager = context.require(ViewManager.class);

                boolean executionPassed = execution.getResult() == ReportNodeStatus.PASSED;

                ingestionPipeline.ingestPoint(withExecutionAttributes(execution, Map.of(METRIC_TYPE, EXECUTIONS_COUNT)), execution.getStartTime(), 1);
                ingestionPipeline.ingestPoint(withExecutionAttributes(execution, Map.of(METRIC_TYPE, FAILURE_PERCENTAGE)), execution.getStartTime(), executionPassed ? 0 : 100);
                ingestionPipeline.ingestPoint(withExecutionAttributes(execution, Map.of(METRIC_TYPE, FAILURE_COUNT)), execution.getStartTime(), executionPassed ? 0 : 1);

                ErrorDistribution errorDistribution = (ErrorDistribution) viewManager.queryView(ErrorDistributionView.ERROR_DISTRIBUTION_VIEW, context.getExecutionId());

                errorDistribution.getCountByErrorCode().entrySet().forEach(entry -> {
                    ingestionPipeline.ingestPoint(withExecutionAttributes(execution, Map.of(METRIC_TYPE, FAILURES_COUNT_BY_ERROR_CODE, ERROR_CODE, entry.getKey())), execution.getStartTime(), entry.getValue() > 0 ? 1 : 0);
                });
                // Ensure that all measurements have been flushed before the execution ends
                // This is critical for the SchedulerTaskAssertions to work properly
                mainIngestionPipeline.flush();
            }
        };
    }

    private BucketAttributes withExecutionAttributes(Execution execution, Map<String, Object> attributes) {
        HashMap<String, Object> result = new HashMap<>(attributes);
        result.put(EXECUTION_ID, execution.getId().toString());
        String executionTaskID = execution.getExecutionTaskID();
        if (executionTaskID != null) {
            result.put(TASK_ID, executionTaskID);
        }
        result.put(PLAN_ID, execution.getPlanId());
        return new BucketAttributes(result);
    }

    @Override
    public void serverStop(GlobalContext context) {
        mainIngestionPipeline.close();
    }
}
