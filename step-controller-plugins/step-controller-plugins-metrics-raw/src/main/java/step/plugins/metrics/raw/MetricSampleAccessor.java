package step.plugins.metrics.raw;

import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.collections.filters.Equals;
import step.core.metrics.MetricsExecutionPlugin;
import step.core.metrics.ExecutionMetricSample;

import java.util.Map;
import java.util.stream.Stream;

public class MetricSampleAccessor extends AbstractAccessor<ExecutionMetricSample> {

    public MetricSampleAccessor(Collection<ExecutionMetricSample> collectionDriver) {
        super(collectionDriver);
    }

    public void removeExecutionMetrics(String executionId) {
        Equals executionIdFilter = Filters.equals(MetricsExecutionPlugin.ATTRIBUTE_EXECUTION_ID, executionId);
        this.collectionDriver.remove(executionIdFilter);
    }

    public Stream<ExecutionMetricSample> findByReportNodeId(String rnId) {
        Equals executionIdFilter = Filters.equals(MetricsExecutionPlugin.RN_ID, rnId);
        return this.findManyByCriteria(Map.of(MetricsExecutionPlugin.RN_ID, rnId));
    }
}
