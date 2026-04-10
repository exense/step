package step.plugins.measurements;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;

import java.util.List;
import java.util.Map;

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
    default void processMetrics(List<ExecutionMetricSample> metrics) {
        processMetrics(metrics, null);
    }

    /**
     * Processes a batch of enriched metric snapshots (counter, gauge, histogram) providing a set of optional labels as keys/values.
     * The optional labels are usually redundant for internal handlers but can be used for external handlers to enrich the metrics (i.e. the Prometheus handler)
     * <p>
     * Called both for end-of-keyword output metrics (from
     * {@link step.functions.io.OutputBuilder#addMetric}) and for live metric snapshots
     * dispatched periodically during keyword execution.
     * <p>
     * The default implementation invoke {@link #processMetrics(List)} discarding the optionalLabels
     *
     * @param metrics enriched metric snapshots, never {@code null}
     * @param optionalLabels Map of optional labels that be used to further enrich the metric snapshots, can be {@code null}
     */
    default void processMetrics(List<ExecutionMetricSample> metrics, Map<String, String> optionalLabels) {
    }

    /**
     * Processes a batch of controller-level metric snapshots that carry no execution
     * context (e.g. grid token usage, system gauges).
     * <p>
     * The default no-op implementation allows handlers that do not need controller
     * metrics to remain unchanged.
     *
     * @param metrics controller-level metric snapshots, never {@code null}
     */
    default void processControllerMetrics(List<ControllerMetricSample> metrics) {
    }

    void afterExecutionEnd(ExecutionContext context);

}
