package step.plugins.timeseries;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.metrics.InstrumentType;
import step.core.metrics.MetricSample;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.bucket.Bucket;
import step.core.timeseries.bucket.BucketAttributes;
import step.core.timeseries.ingestion.TimeSeriesIngestionPipeline;
import step.core.metrics.AbstractMetricSample;
import step.core.metrics.ControllerMetricSample;
import step.core.metrics.Measurement;
import step.core.metrics.MetricHeartbeatRegistry;
import step.core.metrics.MetricSamplesHandler;
import step.core.metrics.ExecutionMetricSample;

import java.util.*;

import static step.core.metrics.AbstractMetricSample.METRIC_TYPE;
import static step.core.metrics.MetricsControllerPlugin.RESPONSE_TIME;
import static step.core.metrics.MetricsControllerPlugin.THREAD_GROUP;

/**
 * This class acts as a wrapper over a TimeSeries ingestion. It has special methods which alter the data before ingestion.
 */
public class TimeSeriesMetricSamplesHandler implements MetricSamplesHandler {

    private final TimeSeries timeSeries;

    private final Set<String> handledAttributes;
    private final Set<String> excludedAttributes;

    public TimeSeriesMetricSamplesHandler(TimeSeries timeSeries, Set<String> handledAttributes, Set<String> excludedAttributes) {
        this.timeSeries = timeSeries;
        this.handledAttributes = Objects.requireNonNull(handledAttributes);
        this.excludedAttributes = Objects.requireNonNull(excludedAttributes);
        if (!handledAttributes.isEmpty() &&  !excludedAttributes.isEmpty()) {
            throw new IllegalArgumentException("Either a set of handled attributes or a set of excluded attributes is required, setting both is not supported.");
        }
        MetricHeartbeatRegistry.getInstance().registerHandler(this);
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
        bucketAttributes.put(METRIC_TYPE, RESPONSE_TIME);
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

    public void processThreadGroupAsMeasurement(Measurement measurement) {
        if (measurement != null) {
            BucketAttributes bucketAttributes = measurementToBucketAttributes(measurement);
            bucketAttributes.put(METRIC_TYPE, measurement.getType());
            bucketAttributes.put("instrumentType", InstrumentType.GAUGE.toLowerCase());
            TimeSeriesIngestionPipeline ingestionPipeline = this.timeSeries.getIngestionPipeline();
            ingestionPipeline.ingestPoint(bucketAttributes, measurement.getBegin(), measurement.getValue());
        }
    }

    /**
     * Ingests a batch of enriched metric samples into the time series.
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
    public void processMetrics(List<ExecutionMetricSample> metrics) {
        metrics.forEach(this::processMetric);
    }

    @Override
    public void processControllerMetrics(List<ControllerMetricSample> metrics) {
        metrics.forEach(this::processMetric);
    }

    public void processMetric(AbstractMetricSample mm) {
        MetricSample sample = mm.sample;
        long begin = sample.getSampleTime();
        BucketAttributes attributes = metricSampleToBucketAttributes(mm);
        String instrumentType = sample.getType().toLowerCase();
        attributes.put(METRIC_TYPE, mm.metricType != null ? mm.metricType : instrumentType);
        attributes.put("instrumentType", instrumentType);
        TimeSeriesIngestionPipeline ingestionPipeline = timeSeries.getIngestionPipeline();
        ingestionPipeline.ingestBucket(buildMetricBucket(attributes, begin, sample));
    }

    private BucketAttributes metricSampleToBucketAttributes(AbstractMetricSample mm) {
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

    private static Bucket buildMetricBucket(BucketAttributes attributes, long begin, MetricSample sample) {
        Bucket bucket = new Bucket();
        bucket.setBegin(begin);
        bucket.setAttributes(attributes);
        bucket.setCount(sample.getCount());
        bucket.setSum(sample.getSum());
        bucket.setMin(sample.getMin());
        bucket.setMax(sample.getMax());
        if (sample.getDistribution() != null) {
            bucket.setDistribution(sample.getDistribution());
        }
        return bucket;
    }

    /**
     * This method will handle existing measurements, and will check if it is a gauge or normal measurement
     *
     * @param measurement the RAW measurement to be ingested
     */
    public void ingestExistingMeasurement(Measurement measurement) {
        if (measurement == null) {
            return;
        }
        measurement.remove("_id"); // because these measurements come with a generated id and can't be grouped into buckets.
        if (measurement.getType().equals(THREAD_GROUP)) {
            //Convert to the ThreadGroup Metric Sample
            processThreadGroupAsMeasurement(measurement);
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
