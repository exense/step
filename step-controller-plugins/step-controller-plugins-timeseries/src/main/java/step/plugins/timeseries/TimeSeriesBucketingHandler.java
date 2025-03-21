package step.plugins.timeseries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.bucket.BucketAttributes;
import step.core.timeseries.ingestion.TimeSeriesIngestionPipeline;
import step.plugins.measurements.Measurement;
import step.plugins.measurements.MeasurementHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class acts as a wrapper over a TimeSeries ingestion. It has special methods which alter the data before ingestion.
 */
public class TimeSeriesBucketingHandler implements MeasurementHandler {

    private static final String THREAD_GROUP_MEASUREMENT_TYPE = "threadgroup";
    private static final String METRIC_TYPE_KEY = "metricType";
    private static final String METRIC_TYPE_RESPONSE_TIME = "response-time";
    private static final String METRIC_TYPE_SAMPLER = "sampler";

    private static final Logger logger = LoggerFactory.getLogger(TimeSeriesBucketingHandler.class);

    private final TimeSeries timeSeries;

    private final List<String> handledAttributes;

    public TimeSeriesBucketingHandler(TimeSeries timeSeries, List<String> handledAttributes) {
        this.timeSeries = timeSeries;
        this.handledAttributes = handledAttributes;
    }

    @Override
    public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {

    }

    @Override
    public void processMeasurements(List<Measurement> measurements) {
        measurements.forEach(this::processMeasurement);
    }

    public void processMeasurement(Measurement measurement) {
        long begin = measurement.getBegin();
        long value = measurement.getValue();

        BucketAttributes bucketAttributes = measurementToBucketAttributes(measurement);
        bucketAttributes.put(METRIC_TYPE_KEY, METRIC_TYPE_RESPONSE_TIME);
        TimeSeriesIngestionPipeline ingestionPipeline = this.timeSeries.getIngestionPipeline();
        ingestionPipeline.ingestPoint(bucketAttributes, begin, value);
    }

    private BucketAttributes measurementToBucketAttributes(Measurement measurement) {
        Map<String, Object> bucketAttributesMap = new HashMap<>();
        handledAttributes.forEach(a -> {
            if (measurement.containsKey(a)) {
                bucketAttributesMap.put(a,measurement.get(a));
            }
        });
        return new BucketAttributes(bucketAttributesMap);
    }

    @Override
    public void processGauges(List<Measurement> measurements) {
        measurements.forEach(measurement -> {
            if (measurement != null) {
                BucketAttributes bucketAttributes = measurementToBucketAttributes(measurement);
                bucketAttributes.put(METRIC_TYPE_KEY, measurement.getType());
                TimeSeriesIngestionPipeline ingestionPipeline = this.timeSeries.getIngestionPipeline();
                ingestionPipeline.ingestPoint(bucketAttributes, measurement.getBegin(), measurement.getValue());
            }
        });
    }

    /**
     * This method will handle existing measurements, and will check if it is a gauge or normal measurement
     * @param measurement
     */
    public void ingestExistingMeasurement(Measurement measurement) {
        if (measurement == null) {
            return;
        }
        measurement.remove("_id"); // because these measurements come with a generated id and can't be grouped into buckets.
        if (measurement.getType().equals(THREAD_GROUP_MEASUREMENT_TYPE)) {
            this.processGauges(List.of(measurement));
        } else {
            this.processMeasurement(measurement);
        }
    }

    @Override
    public void afterExecutionEnd(ExecutionContext context) {
    }

    public List<String> getHandledAttributes() {
        return handledAttributes;
    }

    public void flush() {
        this.timeSeries.getIngestionPipeline().flush();
    }
}
