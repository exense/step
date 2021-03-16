package step.plugins.measurements;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;

import java.util.List;

public interface MeasurementHandler {


	void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext);
	void processMeasurements(List<Measurement> measurements);
	void processGauges( List<Measurement> measurements);
	void afterExecutionEnd(ExecutionContext context);

}
