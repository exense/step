package step.plugins.measurements;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;

import java.util.List;

public interface MeasurementHandler {

    void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext);

    void processMeasurements(List<Measurement> measurements);

    /**
     * Receives {@link Measurement}s produced by internal Prometheus {@link io.prometheus.client.Gauge}s
     * (e.g. thread-group concurrency, agent token usage) that are already exposed as native
     * Prometheus metrics. Handlers that need to persist these (time-series, raw storage) should
     * delegate to {@link #processMeasurements}. Handlers that would otherwise duplicate the
     * Prometheus exposure (e.g. PrometheusHandler) should override this as a no-op.
     *
     * <p>The default implementation delegates to {@link #processMeasurements} so that existing
     * handlers continue to store internal gauge data without any changes.
     */
    default void processInternalGauges(List<Measurement> measurements) {
        processMeasurements(measurements);
    }

    void afterExecutionEnd(ExecutionContext context);

}
