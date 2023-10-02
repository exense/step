package step.plugins.timeseries;

import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.plugins.Plugin;
import step.core.timeseries.TimeSeriesIngestionPipeline;
import step.core.timeseries.bucket.BucketAttributes;
import step.core.views.ViewManager;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.plugins.views.functions.ErrorDistribution;
import step.plugins.views.functions.ErrorDistributionView;

import java.util.HashMap;
import java.util.Map;

@Plugin(dependencies= {})
public class TimeSeriesExecutionPlugin extends AbstractExecutionEnginePlugin {

	// TODO find a better place to define this constant
	public static final String METRIC_TYPE = "metricType";

	public static final String EXECUTIONS_COUNT = "executions/count";
	public static final String FAILURE_PERCENTAGE = "executions/failure-percentage";
	public static final String FAILURE_COUNT = "executions/failure-count";
	public static final String FAILURES_COUNT_BY_ERROR_CODE = "executions/failures-count-by-error-code";
	public static final String ERROR_CODE = "errorCode";
	public static String TIMESERIES_FLAG = "hasTimeSeries";

	public static final String EXECUTION_ID = "eId";
	public static final String TASK_ID = "taskId";
	public static final String PLAN_ID = "planId";

	@Override
	public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
		super.initializeExecutionContext(executionEngineContext, executionContext);
		Execution execution = executionContext.getExecutionAccessor().get(executionContext.getExecutionId());
		if (execution != null) {
			execution.addCustomField(TIMESERIES_FLAG,true);
			executionContext.getExecutionAccessor().save(execution);
		}
	}

}
