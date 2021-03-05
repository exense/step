package step.plugins.measurements;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;

import java.util.List;

public interface MeasurementHandler {


	void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext);
	void processMeasurements(List<Measurement> measurements, ExecutionContext executionContext);
	void processGauges(GaugeCollector collector, List<GaugeCollector.GaugeMetric> metrics);
	void afterExecutionEnd(ExecutionContext context);

}
