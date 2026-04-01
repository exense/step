package step.plugins.measurements;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;

import java.util.List;

public interface SamplesHandler {

    void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext);

    void processMeasurements(List<Measurement> measurements);

    void processGauges(List<Measurement> measurements);

    /**
     * Processes a batch of enriched metric snapshots (counter, gauge, histogram).
     * <p>
     * Called both for end-of-keyword output metrics (from
     * {@link step.functions.io.OutputBuilder#addMetric}) and for live metric snapshots
     * dispatched periodically during keyword execution.
     * <p>
     * The default no-op implementation allows existing handlers to remain unchanged
     * until they opt-in to metric processing.
     *
     * @param metrics enriched metric snapshots, never {@code null}
     */
    default void processMetrics(List<StepMetricSample> metrics) {
    }

    void afterExecutionEnd(ExecutionContext context);

}
