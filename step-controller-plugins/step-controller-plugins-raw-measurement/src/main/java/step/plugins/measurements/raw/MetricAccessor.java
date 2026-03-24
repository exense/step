package step.plugins.measurements.raw;

import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.collections.filters.Equals;
import step.plugins.measurements.MeasurementPlugin;
import step.plugins.measurements.MetricMeasurement;

public class MetricAccessor extends AbstractAccessor<MetricMeasurement> {

    public MetricAccessor(Collection<MetricMeasurement> collectionDriver) {
        super(collectionDriver);
    }

    public void removeExecutionMetrics(String executionId) {
        Equals executionIdFilter = Filters.equals(MeasurementPlugin.ATTRIBUTE_EXECUTION_ID, executionId);
        this.collectionDriver.remove(executionIdFilter);
    }
}
