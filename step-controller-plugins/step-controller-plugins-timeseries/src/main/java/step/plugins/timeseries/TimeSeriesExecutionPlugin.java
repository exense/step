package step.plugins.timeseries;

import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.type.ExecutionTypeManager;
import step.core.execution.type.ExecutionTypePlugin;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.timeseries.TimeSeriesIngestionPipeline;
import step.core.timeseries.aggregation.TimeSeriesAggregationPipeline;
import step.core.timeseries.bucket.BucketAttributes;
import step.core.views.ViewManager;
import step.core.views.ViewPlugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.plugins.views.functions.ErrorDistribution;
import step.plugins.views.functions.ErrorDistributionView;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@IgnoreDuringAutoDiscovery
@Plugin(dependencies= {ViewPlugin.class, ExecutionTypePlugin.class})
public class TimeSeriesExecutionPlugin extends AbstractExecutionEnginePlugin {

	// TODO find a better place to define this constant
	public static final String METRIC_TYPE = "metricType";

	public static final String EXECUTIONS_COUNT = "executions/count";
	public static final String FAILURE_PERCENTAGE = "executions/failure-percentage";
	public static final String FAILURE_COUNT = "executions/failure-count";
	public static final String FAILURES_COUNT_BY_ERROR_CODE = "executions/failures-count-by-error-code";
	public static final String RESPONSE_TIME = "response-time";
	public static final String ERROR_CODE = "errorCode";
	public static String TIMESERIES_FLAG = "hasTimeSeries";

	public static final String EXECUTION_ID = "eId";
	public static final String TASK_ID = "taskId";
	public static final String PLAN_ID = "planId";

	private final TimeSeriesIngestionPipeline parentIngestionPipeline;
	private final TimeSeriesAggregationPipeline aggregationPipeline;

	public TimeSeriesExecutionPlugin(TimeSeriesIngestionPipeline parentIngestionPipeline, TimeSeriesAggregationPipeline aggregationPipeline) {
		this.parentIngestionPipeline = parentIngestionPipeline;
		this.aggregationPipeline = aggregationPipeline;
	}

	@Override
	public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
		super.initializeExecutionContext(executionEngineContext, executionContext);
		TreeMap<String, String> additionalAttributes = executionContext.getObjectEnricher().getAdditionalAttributes();
		TimeSeriesIngestionPipeline ingestionPipeline = new TimeSeriesIngestionPipeline(null, 0) {
			@Override
			public void ingestPoint(Map<String, Object> attributes, long timestamp, long value) {
				attributes.putAll(additionalAttributes);
				parentIngestionPipeline.ingestPoint(attributes, timestamp, value);
			}

			@Override
			public void ingestPoint(BucketAttributes attributes, long timestamp, long value) {
				attributes.putAll(additionalAttributes);
				parentIngestionPipeline.ingestPoint(attributes, timestamp, value);
			}

			@Override
			public void flush() {
				parentIngestionPipeline.flush();
			}

			@Override
			public long getFlushCount() {
				return parentIngestionPipeline.getFlushCount();
			}

			@Override
			public void close() {
				parentIngestionPipeline.close();
			}
		};

		executionContext.put(TimeSeriesAggregationPipeline.class, aggregationPipeline);
		executionContext.put(TimeSeriesIngestionPipeline.class, ingestionPipeline);

		Execution execution = executionContext.getExecutionAccessor().get(executionContext.getExecutionId());
		if (execution != null) {
			execution.addCustomField(TIMESERIES_FLAG,true);
			executionContext.getExecutionAccessor().save(execution);
		}
	}

	@Override
	public void afterExecutionEnd(ExecutionContext context) {
		TimeSeriesIngestionPipeline ingestionPipeline = context.require(TimeSeriesIngestionPipeline.class);
		ExecutionAccessor executionAccessor = context.getExecutionAccessor();
		Execution execution = executionAccessor.get(context.getExecutionId());
		ViewManager viewManager = context.require(ViewManager.class);
		ExecutionTypeManager executionTypeManager = context.require(ExecutionTypeManager.class);

		if (executionTypeManager.get(execution.getExecutionType()).generateExecutionMetrics()) {
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
		return new BucketAttributes(result);
	}
}
