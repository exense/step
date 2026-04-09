package step.plugins.measurements.raw;

import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.collections.filters.Equals;
import step.plugins.measurements.SamplesExecutionPlugin;
import step.plugins.measurements.ExecutionMetricSample;

import java.util.Map;
import java.util.stream.Stream;

public class MetricSampleAccessor extends AbstractAccessor<ExecutionMetricSample> {

    public MetricSampleAccessor(Collection<ExecutionMetricSample> collectionDriver) {
        super(collectionDriver);
    }

    public void removeExecutionMetrics(String executionId) {
        Equals executionIdFilter = Filters.equals(SamplesExecutionPlugin.ATTRIBUTE_EXECUTION_ID, executionId);
        this.collectionDriver.remove(executionIdFilter);
    }

    public Stream<ExecutionMetricSample> findByReportNodeId(String rnId) {
        Equals executionIdFilter = Filters.equals(SamplesExecutionPlugin.RN_ID, rnId);
        return this.findManyByCriteria(Map.of(SamplesExecutionPlugin.RN_ID, rnId));
    }
}
