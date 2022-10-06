package step.plugins.timeseries;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.model.Execution;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;

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

}
