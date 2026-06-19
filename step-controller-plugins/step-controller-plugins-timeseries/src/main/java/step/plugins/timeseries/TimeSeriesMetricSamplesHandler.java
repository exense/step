package step.plugins.timeseries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ExecutionNotice;
import step.core.execution.notices.ExecutionNoticeManager;
import step.core.metrics.InstrumentType;
import step.core.metrics.MetricSample;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.bucket.Bucket;
import step.core.timeseries.bucket.BucketAttributes;
import step.core.timeseries.ingestion.TimeSeriesIngestionPipeline;
import step.core.metrics.StepMetricSample;
import step.core.metrics.ControllerMetricSample;
import step.core.metrics.Measurement;
import step.core.metrics.MetricHeartbeatRegistry;
import step.core.metrics.MetricSamplesHandler;
import step.core.metrics.ExecutionMetricSample;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static step.core.metrics.StepMetricSample.METRIC_TYPE;
import static step.core.metrics.MetricsConstants.INSTRUMENT_TYPE_ATTRIBUTE;
import static step.core.metrics.MetricsControllerPlugin.RESPONSE_TIME;
import static step.core.metrics.MetricsControllerPlugin.THREAD_GROUP;

/**
 * This class acts as a wrapper over a TimeSeries ingestion. It has special methods which alter the data before ingestion.
 */
public class TimeSeriesMetricSamplesHandler implements MetricSamplesHandler {

    private static final Logger logger = LoggerFactory.getLogger(TimeSeriesMetricSamplesHandler.class);

    /** Static value substituted for label values dropped once a label exceeds its unique-value quota. */
    public static final String QUOTA_EXCEEDED_VALUE = "values dismissed due to quota exceeded";

    /** Id of the execution notice type raised when a label's unique-value quota is exceeded. */
    public static final String CARDINALITY_NOTICE_TYPE_ID = "timeseries.label-cardinality-quota-exceeded";

    private final TimeSeries timeSeries;

    private final Set<String> handledAttributes;
    private final Set<String> excludedAttributes;

    /**
     * Maximum number of unique values tolerated per (execution, metric name, label name) before new
     * values are masked with {@link #QUOTA_EXCEEDED_VALUE}. A value {@code <= 0} disables the safeguard.
     */
    private final int maxUniqueLabelValues;

    /**
     * In-memory cardinality tracking, bounded to the lifecycle of an execution and cleaned up in
     * {@link #afterExecutionEnd(ExecutionContext)}:
     * {@code executionId -> metricName -> labelName -> set of observed values}.
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, Set<String>>>> labelValueTracking = new ConcurrentHashMap<>();

    /**
     * Tracks the {@code metricName + '\0' + labelName} keys for which a quota warning has already been
     * raised, per execution, so the user is notified only once per label.
     */
    private final ConcurrentHashMap<String, Set<String>> warnedLabels = new ConcurrentHashMap<>();

    /**
     * Used to raise an execution notice the first time a label exceeds its quota. May be {@code null}
     * for re-ingestion/rebuild and test paths, in which case the quota breach is only logged.
     */
    private final ExecutionNoticeManager executionNoticeManager;

    /**
     * @param timeSeries         the time series to ingest metric samples into
     * @param handledAttributes  allowlist of attribute keys to retain when building bucket
     *                           attributes from a measurement or metric sample label set.
     *                           If non-empty, only these keys are forwarded; all others are
     *                           dropped. Must be empty when {@code excludedAttributes} is
     *                           non-empty.
     * @param excludedAttributes denylist of attribute keys to strip when building bucket
     *                           attributes. If non-empty, all keys from the source are
     *                           forwarded <em>except</em> those listed here. Must be empty
     *                           when {@code handledAttributes} is non-empty.
     *                           <p>
     *                           When both sets are empty, all attributes are forwarded
     *                           without filtering. Providing both as non-empty sets is not
     *                           supported and throws {@link IllegalArgumentException}.
     */
    public TimeSeriesMetricSamplesHandler(TimeSeries timeSeries, Set<String> handledAttributes, Set<String> excludedAttributes) {
        // Cardinality safeguard disabled by default: used by re-ingestion/rebuild paths (raw data was
        // already filtered at first ingestion) and by tests that don't exercise the quota.
        this(timeSeries, handledAttributes, excludedAttributes, 0, null);
    }

    /**
     * @param maxUniqueLabelValues maximum number of unique values per (execution, metric name, label name)
     *                             before new user-defined label values are masked. {@code <= 0} disables the safeguard.
     */
    public TimeSeriesMetricSamplesHandler(TimeSeries timeSeries, Set<String> handledAttributes, Set<String> excludedAttributes, int maxUniqueLabelValues) {
        this(timeSeries, handledAttributes, excludedAttributes, maxUniqueLabelValues, null);
    }

    /**
     * @param maxUniqueLabelValues   maximum number of unique values per (execution, metric name, label name)
     *                               before new user-defined label values are masked. {@code <= 0} disables the safeguard.
     * @param executionNoticeManager manager used to raise an execution notice the first time a label exceeds
     *                               its quota; may be {@code null} (the breach is then only logged).
     */
    public TimeSeriesMetricSamplesHandler(TimeSeries timeSeries, Set<String> handledAttributes, Set<String> excludedAttributes, int maxUniqueLabelValues, ExecutionNoticeManager executionNoticeManager) {
        this.timeSeries = timeSeries;
        this.handledAttributes = Objects.requireNonNull(handledAttributes);
        this.excludedAttributes = Objects.requireNonNull(excludedAttributes);
        this.maxUniqueLabelValues = maxUniqueLabelValues;
        this.executionNoticeManager = executionNoticeManager;
        if (!handledAttributes.isEmpty() && !excludedAttributes.isEmpty()) {
            throw new IllegalArgumentException("Either a set of handled attributes or a set of excluded attributes is required, setting both is not supported.");
        }
        MetricHeartbeatRegistry.getInstance().registerHandler(this);
    }

    @Override
    public void processMeasurements(ExecutionContext executionContext, List<Measurement> measurements) {
        measurements.forEach(m -> processMeasurement(executionContext, m));
    }

    /** Re-ingestion entry point (no execution context, no cardinality safeguard). */
    public void processMeasurement(Measurement measurement) {
        processMeasurement(null, measurement);
    }

    public void processMeasurement(ExecutionContext executionContext, Measurement measurement) {
        long begin = measurement.getBegin();
        long value = measurement.getValue();

        BucketAttributes bucketAttributes = measurementToBucketAttributes(executionContext, measurement);
        bucketAttributes.put(METRIC_TYPE, RESPONSE_TIME);
        TimeSeriesIngestionPipeline ingestionPipeline = this.timeSeries.getIngestionPipeline();
        ingestionPipeline.ingestPoint(bucketAttributes, begin, value);
    }

    private BucketAttributes measurementToBucketAttributes(ExecutionContext executionContext, Measurement measurement) {
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
        // Apply the cardinality safeguard to the keyword-author-defined custom data labels only.
        applyLabelQuota(executionContext, measurement.getExecId(), measurement.getName(),
            measurement.getCustomMetricLabelKeys(), bucketAttributesMap);
        return new BucketAttributes(bucketAttributesMap);
    }

    private void processThreadGroupAsMeasurement(Measurement measurement) {
        if (measurement != null) {
            BucketAttributes bucketAttributes = measurementToBucketAttributes(null, measurement);
            bucketAttributes.put(METRIC_TYPE, measurement.getType());
            bucketAttributes.put(INSTRUMENT_TYPE_ATTRIBUTE.getName(), InstrumentType.GAUGE.toLowerCase());
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
    public void processMetrics(ExecutionContext executionContext, List<ExecutionMetricSample> metrics) {
        metrics.forEach(mm -> processMetric(executionContext, mm));
    }

    @Override
    public void processControllerMetrics(List<ControllerMetricSample> metrics) {
        // Controller metrics are not bound to an execution and carry no user-defined labels.
        metrics.forEach(mm -> processMetric(null, mm));
    }

    /** Re-ingestion entry point (no execution context, no cardinality safeguard). */
    public void processMetric(StepMetricSample mm) {
        processMetric(null, mm);
    }

    public void processMetric(ExecutionContext executionContext, StepMetricSample mm) {
        MetricSample sample = mm.sample;
        long begin = sample.getSampleTime();
        BucketAttributes attributes = metricSampleToBucketAttributes(executionContext, mm);
        String instrumentType = sample.getType().toLowerCase();
        attributes.put(METRIC_TYPE, mm.metricType != null ? mm.metricType : instrumentType);
        attributes.put(INSTRUMENT_TYPE_ATTRIBUTE.getName(), instrumentType);
        TimeSeriesIngestionPipeline ingestionPipeline = timeSeries.getIngestionPipeline();
        ingestionPipeline.ingestBucket(buildMetricBucket(attributes, begin, sample));
    }

    private BucketAttributes metricSampleToBucketAttributes(ExecutionContext executionContext, StepMetricSample mm) {
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
        // Apply the cardinality safeguard to the keyword-developer-defined labels only (sample.getLabels()),
        // not to the context labels merged in by getEffectiveLabels(). Only execution metrics are tracked.
        if (mm instanceof ExecutionMetricSample) {
            ExecutionMetricSample ems = (ExecutionMetricSample) mm;
            Set<String> userLabelKeys = (mm.sample.getLabels() != null) ? mm.sample.getLabels().keySet() : null;
            applyLabelQuota(executionContext, ems.eId, mm.sample.getName(), userLabelKeys, attributesMap);
        }
        return new BucketAttributes(attributesMap);
    }

    /**
     * Enforces the per-execution unique-value quota on the given user-defined label keys, mutating
     * {@code attributes} in place. Values beyond the quota are replaced with {@link #QUOTA_EXCEEDED_VALUE}.
     * No-op when the safeguard is disabled, when there is no execution scope, or when there are no
     * user-defined labels to police.
     */
    private void applyLabelQuota(ExecutionContext executionContext, String execId, String metricName,
                                 Set<String> userLabelKeys, Map<String, Object> attributes) {
        if (maxUniqueLabelValues <= 0 || execId == null || execId.isEmpty()
            || userLabelKeys == null || userLabelKeys.isEmpty()) {
            return;
        }
        String name = (metricName != null) ? metricName : "";
        for (String labelName : userLabelKeys) {
            Object value = attributes.get(labelName);
            if (value == null) {
                continue;
            }
            String masked = maskIfOverQuota(executionContext, execId, name, labelName, String.valueOf(value));
            attributes.put(labelName, masked);
        }
    }

    /**
     * Atomically records the value for the given (execution, metric, label) tuple and decides whether it
     * passes through or must be masked:
     * <ul>
     *   <li>known value, or new value while under quota → returned unchanged (and recorded);</li>
     *   <li>new value once the quota is reached → returned as {@link #QUOTA_EXCEEDED_VALUE}, and a warning
     *       is raised once per label.</li>
     * </ul>
     */
    private String maskIfOverQuota(ExecutionContext executionContext, String execId, String metricName,
                                   String labelName, String value) {
        Set<String> values = labelValueTracking
            .computeIfAbsent(execId, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(metricName, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(labelName, k -> ConcurrentHashMap.newKeySet());
        if (values.contains(value)) {
            return value;
        }
        // Serialize the size-check-and-add so concurrent live ingestion can't overshoot the quota.
        synchronized (values) {
            if (values.contains(value)) {
                return value;
            }
            if (values.size() < maxUniqueLabelValues) {
                values.add(value);
                return value;
            }
        }
        raiseQuotaWarningOnce(executionContext, execId, metricName, labelName);
        return QUOTA_EXCEEDED_VALUE;
    }

    private void raiseQuotaWarningOnce(ExecutionContext executionContext, String execId, String metricName, String labelName) {
        Set<String> warned = warnedLabels.computeIfAbsent(execId, k -> ConcurrentHashMap.newKeySet());
        if (!warned.add(metricName + '\0' + labelName)) {
            return;
        }
        logger.warn("Execution {}: high cardinality detected on custom metric label [{}] for metric [{}]. " +
                "Unique values exceeding the quota of {} have been dismissed (replaced with \"{}\").",
            execId, labelName, metricName, maxUniqueLabelValues, QUOTA_EXCEEDED_VALUE);
        if (executionNoticeManager != null) {
            ExecutionNotice notice = new ExecutionNotice(CARDINALITY_NOTICE_TYPE_ID, Map.of(
                "labelName", labelName,
                "metricName", metricName,
                "quota", String.valueOf(maxUniqueLabelValues)));
            executionNoticeManager.raiseNotice(executionContext, notice);
        }
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

    public Set<String> getHandledAttributes() {
        return handledAttributes;
    }

    public Set<String> getExcludedAttributes() {
        return excludedAttributes;
    }

    public void flush() {
        this.timeSeries.getIngestionPipeline().flush();
    }

    @Override
    public void afterExecutionEnd(ExecutionContext context) {
        // Explicitly release the per-execution cardinality tracking state as soon as the execution ends.
        if (context != null) {
            String executionId = context.getExecutionId();
            labelValueTracking.remove(executionId);
            warnedLabels.remove(executionId);
        }
    }
}
