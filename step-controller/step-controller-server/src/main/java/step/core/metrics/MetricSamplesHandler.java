package step.core.metrics;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;

import java.util.List;
import java.util.Map;

public interface MetricSamplesHandler {

    default void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {

    }

    /**
     * Processes a batch of measurements.
     *
     * @param executionContext the execution context the measurements were produced in, or {@code null}
     *                         when they are not bound to a running execution (e.g. periodic re-ingestion).
     *                         Handlers may use it to surface execution-level feedback (warnings, status).
     * @param measurements     the measurements to process, never {@code null}
     */
    void processMeasurements(ExecutionContext executionContext, List<Measurement> measurements);

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
     * @param executionContext the execution context the metrics were produced in, or {@code null}
     *                         when they are not bound to a running execution (e.g. metric heartbeats).
     * @param metrics          enriched metric snapshots, never {@code null}
     */
    default void processMetrics(ExecutionContext executionContext, List<ExecutionMetricSample> metrics) {

    }

    /**
     * Processes a batch of enriched metric snapshots (counter, gauge, histogram) providing a set of optional labels as keys/values.
     * The optional labels are usually redundant for internal handlers but can be used for external handlers to enrich the metrics (i.e. the Prometheus handler)
     * <p>
     * Called both for end-of-keyword output metrics (from
     * {@link step.functions.io.OutputBuilder#addMetric}) and for live metric snapshots
     * dispatched periodically during keyword execution.
     * <p>
     * The default implementation invoke {@link #processMetrics(ExecutionContext, List)} discarding the optionalLabels
     *
     * @param executionContext the execution context the metrics were produced in, or {@code null}
     * @param metrics          enriched metric snapshots, never {@code null}
     * @param optionalLabels   Map of optional labels that be used to further enrich the metric snapshots, can be {@code null}
     */
    default void processMetrics(ExecutionContext executionContext, List<ExecutionMetricSample> metrics, Map<String, String> optionalLabels) {
        processMetrics(executionContext, metrics);
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

    default void afterExecutionEnd(ExecutionContext context) {

    }

}
