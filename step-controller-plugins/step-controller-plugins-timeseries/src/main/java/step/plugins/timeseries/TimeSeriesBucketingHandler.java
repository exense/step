package step.plugins.timeseries;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.metrics.MetricSample;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.bucket.Bucket;
import step.core.timeseries.bucket.BucketAttributes;
import step.core.timeseries.ingestion.TimeSeriesIngestionPipeline;
import step.plugins.measurements.Measurement;
import step.plugins.measurements.SamplesHandler;
import step.plugins.measurements.StepMetricSample;

import java.util.*;

/**
 * This class acts as a wrapper over a TimeSeries ingestion. It has special methods which alter the data before ingestion.
 */
public class TimeSeriesBucketingHandler implements SamplesHandler {

    private static final String THREAD_GROUP_MEASUREMENT_TYPE = "threadgroup";
    private static final String METRIC_TYPE_KEY = "metricType";
    private static final String METRIC_TYPE_RESPONSE_TIME = "response-time";

    private final TimeSeries timeSeries;

    private final Set<String> handledAttributes;
    private final Set<String> excludedAttributes;

    public TimeSeriesBucketingHandler(TimeSeries timeSeries, Set<String> handledAttributes, Set<String> excludedAttributes) {
        this.timeSeries = timeSeries;
        this.handledAttributes = Objects.requireNonNull(handledAttributes);
        this.excludedAttributes = Objects.requireNonNull(excludedAttributes);
        if (!handledAttributes.isEmpty() &&  !excludedAttributes.isEmpty()) {
            throw new IllegalArgumentException("Either a set of handled attributes or a set of excluded attributes is required, setting both is not supported.");
        }
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
        if (handledAttributes.isEmpty()) {
            bucketAttributesMap.putAll(measurement);
            excludedAttributes.forEach(bucketAttributesMap::remove);
        } else {
            handledAttributes.forEach(a -> {
                if (measurement.containsKey(a)) {
                    bucketAttributesMap.put(a, measurement.get(a));
                }
            });
        }
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
     * Ingests a batch of enriched metric snapshots into the time series.
     * <p>
     * {@link MetricSample} (counter): bucket where {@code count = accumulatedDiff}
     * (increments since last flush, for per-interval rate calculations) and
     * {@code sum = min = max = longRunningTotal} (absolute counter value at the end of the
     * interval, for "current total" display via LAST/MAX aggregation). Empty intervals
     * (diff == 0) are skipped.
     * <p>
     * {@link MetricSample} (gauge/histogram): the pre-aggregated statistics
     * (count, sum, min, max, distribution) are injected directly as a bucket, preserving the
     * full distribution for percentile queries. Empty intervals (count == 0) are skipped.
     */
    @Override
    public void processMetrics(List<StepMetricSample> metrics) {
        metrics.forEach(this::processMetric);
    }

    private void processMetric(StepMetricSample mm) {
        MetricSample sample = mm.sample;
        long begin = sample.getSampleTime();
        BucketAttributes attributes = metricMeasurementToBucketAttributes(mm);
        attributes.put(METRIC_TYPE_KEY, sample.getType().toLowerCase());
        TimeSeriesIngestionPipeline ingestionPipeline = timeSeries.getIngestionPipeline();
        ingestionPipeline.ingestBucket(buildMetricBucket(attributes, begin, sample));
    }

    private BucketAttributes metricMeasurementToBucketAttributes(StepMetricSample mm) {
        Map<String, Object> attributesMap = new HashMap<>();
        Map<String, String> effectiveLabels = mm.getEffectiveLabels();
        if (handledAttributes.isEmpty()) {
            attributesMap.putAll(effectiveLabels);
            excludedAttributes.forEach(attributesMap::remove);
        } else {
            handledAttributes.forEach(a -> {
                if (effectiveLabels.containsKey(a)) {
                    attributesMap.put(a, effectiveLabels.get(a));
                }
            });
        }
        // Metric name always present under "name" for consistent time-series grouping
        attributesMap.put("name", mm.sample.getName());
        return new BucketAttributes(attributesMap);
    }

    private static Bucket buildMetricBucket(BucketAttributes attributes, long begin, MetricSample snapshot) {
        Bucket bucket = new Bucket();
        bucket.setBegin(begin);
        bucket.setAttributes(attributes);
        bucket.setCount(snapshot.getCount());
        bucket.setSum(snapshot.getSum());
        bucket.setMin(snapshot.getMin());
        bucket.setMax(snapshot.getMax());
        if (snapshot.getDistribution() != null) {
            bucket.setDistribution(snapshot.getDistribution());
        }
        return bucket;
    }

    /**
     * This method will handle existing measurements, and will check if it is a gauge or normal measurement
     *
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

    public Set<String> getHandledAttributes() {
        return handledAttributes;
    }

    public Set<String> getExcludedAttributes() {
        return excludedAttributes;
    }

    public void flush() {
        this.timeSeries.getIngestionPipeline().flush();
    }
}
