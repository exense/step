package step.plugins.timeseries;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.timeseries.BucketAttributes;
import step.core.timeseries.TimeSeriesIngestionPipeline;
import step.plugins.measurements.GaugeCollectorRegistry;
import step.plugins.measurements.Measurement;
import step.plugins.measurements.MeasurementHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TimeSeriesBucketingHandler implements MeasurementHandler {

    private static final String THREAD_GROUP_MEASUREMENT_TYPE = "threadgroup";

    private static final Logger logger = LoggerFactory.getLogger(TimeSeriesBucketingHandler.class);

    private final TimeSeriesIngestionPipeline ingestionPipeline;

    public TimeSeriesBucketingHandler(TimeSeriesIngestionPipeline ingestionPipeline) {
        this.ingestionPipeline = ingestionPipeline;
        GaugeCollectorRegistry.getInstance().registerHandler(this);
    }

    @Override
    public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {

    }

    @Override
    public void processMeasurements(List<Measurement> measurements) {
        measurements.forEach(measurement -> {
            BucketAttributes bucketAttributes = new BucketAttributes(measurement);
            // custom fields include all the attributes like execId and planId
            this.ingestionPipeline.ingestPoint(bucketAttributes, measurement.getBegin(), measurement.getValue());
        });
    }

    @Override
    public void processGauges(List<Measurement> measurements) {
        measurements.forEach(measurement -> {
            if (measurement != null && Objects.equals(measurement.getType(), THREAD_GROUP_MEASUREMENT_TYPE)) {
                BucketAttributes bucketAttributes = new BucketAttributes(measurement);
                this.ingestionPipeline.ingestPoint(bucketAttributes, measurement.getBegin(), measurement.getValue());
            }
        });
    }

    @Override
    public void afterExecutionEnd(ExecutionContext context) {
    }
}
