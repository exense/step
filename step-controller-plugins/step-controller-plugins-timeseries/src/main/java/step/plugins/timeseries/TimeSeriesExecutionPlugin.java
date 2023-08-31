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

	public static String TIMESERIES_FLAG = "hasTimeSeries";

	@Override
	public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
		super.initializeExecutionContext(executionEngineContext, executionContext);
		Execution execution = executionContext.getExecutionAccessor().get(executionContext.getExecutionId());
		if (execution != null) {
			execution.addCustomField(TIMESERIES_FLAG,true);
			executionContext.getExecutionAccessor().save(execution);
		}
	}

	@Override
	public void afterExecutionEnd(ExecutionContext context) {
		ExecutionAccessor executionAccessor = context.getExecutionAccessor();
		Execution execution = executionAccessor.get(context.getExecutionId());
		TimeSeriesIngestionPipeline ingestionPipeline = context.require(TimeSeriesIngestionPipeline.class);
		ViewManager viewManager = context.require(ViewManager.class);

		boolean passed = execution.getResult() != ReportNodeStatus.PASSED;

		ingestionPipeline.ingestPoint(withExecutionAttributes(execution, Map.of("metricType", "executions/count")), execution.getStartTime(), 1);
		ingestionPipeline.ingestPoint(withExecutionAttributes(execution, Map.of("metricType", "executions/failure-percentage")), execution.getStartTime(), passed ? 0 : 1);

		ErrorDistribution errorDistribution = (ErrorDistribution) viewManager.queryView(ErrorDistributionView.ERROR_DISTRIBUTION_VIEW, context.getExecutionId());

		errorDistribution.getCountByErrorCode().entrySet().forEach(entry -> {
			ingestionPipeline.ingestPoint(withExecutionAttributes(execution, Map.of("metricType", "executions/failures-count-by-error-code", "errorCode", entry.getKey())), execution.getStartTime(), entry.getValue() > 0 ? 1 : 0);
		});

		super.afterExecutionEnd(context);
	}

	private BucketAttributes withExecutionAttributes(Execution execution, Map<String, Object> attributes) {
		HashMap<String, Object> result = new HashMap<>(attributes);
		result.put("executionId", execution.getId().toString());
		String executionTaskID = execution.getExecutionTaskID();
		if (executionTaskID != null) {
			result.put("taskId", executionTaskID);
		}
		return new BucketAttributes(result);
	}
}
