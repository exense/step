package step.plugins.timeseries;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.type.ExecutionTypeManager;
import step.core.execution.type.ExecutionTypePlugin;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.aggregation.TimeSeriesAggregationPipeline;
import step.core.timeseries.ingestion.TimeSeriesIngestionPipeline;
import step.core.timeseries.TimeSeriesCollectionConfig;
import step.core.views.ViewPlugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;

import java.util.Map;
import java.util.TreeMap;

@IgnoreDuringAutoDiscovery
@Plugin(dependencies = {ViewPlugin.class, ExecutionTypePlugin.class})
public class TimeSeriesExecutionPlugin extends AbstractExecutionEnginePlugin {

    public static String TIMESERIES_FLAG = "hasTimeSeries";

    public static final String EXECUTION_ID = "eId";
    public static final String TASK_ID = "taskId";
    public static final String PLAN_ID = "planId";
    public static final String CANONICAL_PLAN_NAME = "canonicalPlanName";

    private final TimeSeries timeSeries;

    public TimeSeriesExecutionPlugin(TimeSeries timeSeries) {
        this.timeSeries = timeSeries;
    }

    @Override
    public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
        super.initializeExecutionContext(executionEngineContext, executionContext);
        TimeSeriesIngestionPipeline mainIngestionPipeline = timeSeries.getIngestionPipeline();
        TreeMap<String, String> additionalAttributes = executionContext.getObjectEnricher().getAdditionalAttributes();
        //Crete a wrapper of the ingestion pipeline to automatically enrich data with execution attributes
        //This approach is quite error-prone and should be refactored
        TimeSeriesIngestionPipeline ingestionPipeline = new TimeSeriesIngestionPipeline(null,
            new TimeSeriesCollectionConfig().setResolutionMs(mainIngestionPipeline.getResolution())) {
            @Override
            public void ingestPoint(Map<String, Object> attributes, long timestamp, long value) {
                attributes.putAll(additionalAttributes);
                mainIngestionPipeline.ingestPoint(attributes, timestamp, value);
            }

            @Override
            public void flush() {
                mainIngestionPipeline.flush();
            }

            @Override
            public long getFlushCount() {
                return mainIngestionPipeline.getFlushCount();
            }

            @Override
            public void close() {
                mainIngestionPipeline.close();
            }
        };

        executionContext.put(TimeSeriesAggregationPipeline.class, timeSeries.getAggregationPipeline());
        executionContext.put(TimeSeriesIngestionPipeline.class, ingestionPipeline);

        Execution execution = executionContext.getExecutionAccessor().get(executionContext.getExecutionId());
        if (execution != null) {
            execution.addCustomField(TIMESERIES_FLAG, true);
            executionContext.getExecutionAccessor().save(execution);
        }
    }

    @Override
    public void afterExecutionEnd(ExecutionContext context) {
        TimeSeriesIngestionPipeline ingestionPipeline = context.require(TimeSeriesIngestionPipeline.class);
        ExecutionAccessor executionAccessor = context.getExecutionAccessor();
        Execution execution = executionAccessor.get(context.getExecutionId());
        ExecutionTypeManager executionTypeManager = context.require(ExecutionTypeManager.class);

        if (executionTypeManager.get(execution.getExecutionType()).generateExecutionMetrics()) {
            // Ensure that all measurements have been flushed before the execution ends
            // This is critical for the SchedulerTaskAssertions to work properly
            ingestionPipeline.flush();
        }

        super.afterExecutionEnd(context);
    }

    private BucketAttributes withExecutionAttributes(Execution execution, Map<String, Object> attributes) {
        HashMap<String, Object> result = new HashMap<>(attributes);
        result.put(EXECUTION_ID, execution.getId().toString());
        String executionTaskID = execution.getExecutionTaskID();
        if (executionTaskID != null) {
            result.put(TASK_ID, executionTaskID);
        }
        result.put(PLAN_ID, execution.getPlanId());
        result.put(CANONICAL_PLAN_NAME, execution.getImportResult().getCanonicalPlanName());
        return new BucketAttributes(result);
    }
}
