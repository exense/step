package step.core.artefacts.reports.aggregated;

import ch.exense.commons.app.Configuration;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.collections.*;
import step.core.timeseries.*;
import step.core.timeseries.aggregation.TimeSeriesAggregationQueryBuilder;
import step.core.timeseries.aggregation.TimeSeriesOptimizationType;
import step.core.timeseries.bucket.Bucket;
import step.core.timeseries.bucket.BucketAttributes;
import step.core.timeseries.bucket.BucketBuilder;
import step.core.timeseries.ingestion.TimeSeriesIngestionPipeline;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static step.core.timeseries.TimeSeriesConstants.ATTRIBUTES_PREFIX;
import static step.core.timeseries.TimeSeriesConstants.TIMESTAMP_ATTRIBUTE;

public class ReportNodeTimeSeries implements AutoCloseable {

    public static final String CONF_KEY_REPORT_NODE_TIME_SERIES_ENABLED = "execution.engine.reportnodes.timeseries.enabled";
    public static final String TIME_SERIES_MAIN_COLLECTION = "reportNodeTimeSeries";
    public static final String ARTEFACT_HASH = "artefactHash";
    public static final String EXECUTION_ID = "executionId";
    public static final String TYPE = "type";
    public static final String STATUS = "status";
    public static final String ERROR_CODE = "errorCode";
    public static final String ERROR_MESSAGE = "errorMessage";
    private final TimeSeries timeSeries;
    private final TimeSeriesIngestionPipeline ingestionPipeline;
    private final boolean ingestionEnabled;

    public ReportNodeTimeSeries(CollectionFactory collectionFactory, Configuration configuration) {
        this(collectionFactory, TimeSeriesConfig.fromConfiguration(configuration, TIME_SERIES_MAIN_COLLECTION),
            configuration.getPropertyAsBoolean(CONF_KEY_REPORT_NODE_TIME_SERIES_ENABLED, true));
    }

    public ReportNodeTimeSeries(CollectionFactory collectionFactory, TimeSeriesConfig timeSeriesConfig, boolean ingestionEnabled) {
        timeSeries = new TimeSeriesBuilder()
            .withConfig(timeSeriesConfig, collectionFactory, TIME_SERIES_MAIN_COLLECTION, Set.of(EXECUTION_ID))
            .build();
        ingestionPipeline = timeSeries.getIngestionPipeline();
        timeSeries.createIndexes(Set.of(new IndexField(EXECUTION_ID, Order.ASC, String.class)));
        IndexField beginIndexField = new IndexField(TIMESTAMP_ATTRIBUTE, Order.ASC, Long.class);
        timeSeries.createCompoundIndex(new LinkedHashSet<>(List.of(
            new IndexField(ATTRIBUTES_PREFIX + "taskId", Order.ASC, String.class),
            beginIndexField
        )));
        timeSeries.createCompoundIndex(new LinkedHashSet<>(List.of(
            new IndexField(ATTRIBUTES_PREFIX + "planId", Order.ASC, String.class),
            beginIndexField
        )));
        timeSeries.createCompoundIndex(new LinkedHashSet<>(List.of(
            new IndexField(ATTRIBUTES_PREFIX + "canonicalPlanName", Order.ASC, String.class),
            beginIndexField
        )));
        this.ingestionEnabled = ingestionEnabled;
    }

    public TimeSeries getTimeSeries() {
        return timeSeries;
    }

    public void ingestReportNode(ReportNode reportNode) {
        ingestReportNode(reportNode, null);
    }

    public void ingestReportNode(ReportNode reportNode, Map<String, Object> customAttributes) {
        ingestionPipeline.ingestPoint(buildBucketAttributes(reportNode, customAttributes), reportNode.getExecutionTime(), reportNode.getDuration());
    }

    /**
     * Compensates the data ingested for the provided report node by ingesting the opposite of its contribution, with
     * the same attributes and at the same timestamp, so that both cancel each other in the same time bucket of every
     * resolution. It is used when report nodes are removed from the report and must stop counting in the time series,
     * see {@link step.core.artefacts.handlers.ArtefactHandler}
     * <p>
     * The count, the sum and the distribution of the compensated buckets are exact. Their min and their max can not be
     * restored and keep including the compensated samples.
     *
     * @param reportNode       the report node to be compensated, with the same arguments as used for its ingestion
     * @param customAttributes the custom attributes used for its ingestion
     */
    public void compensateReportNode(ReportNode reportNode, Map<String, Object> customAttributes) {
        BucketAttributes attributes = buildBucketAttributes(reportNode, customAttributes);
        long timestamp = reportNode.getExecutionTime();
        // The single sample this report node contributed, built the way it was ingested rather than computed, so that
        // its distribution bracket is guaranteed to be the ingested one
        Bucket ingestedSample = BucketBuilder.create(timestamp).withAttributes(attributes).ingest(reportNode.getDuration()).build();

        // The opposite of that sample: merging it into the ingested data subtracts the sample from the count, from the
        // sum and from its distribution bracket
        Bucket compensation = new Bucket(timestamp);
        compensation.setAttributes(attributes);
        compensation.setCount(-ingestedSample.getCount());
        compensation.setSum(-ingestedSample.getSum());
        compensation.setPclPrecision(ingestedSample.getPclPrecision());
        //Compensate each distribution bracket (negate value)
        if (ingestedSample.getDistribution() != null) {
            compensation.setDistribution(ingestedSample.getDistribution().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> -e.getValue())));
        }
        // Neutral min and max, so that the compensation doesn't contribute to them
        compensation.setMin(Long.MAX_VALUE);
        compensation.setMax(Long.MIN_VALUE);

        ingestionPipeline.ingestBucket(compensation);
    }

    private BucketAttributes buildBucketAttributes(ReportNode reportNode, Map<String, Object> customAttributes) {
        BucketAttributes nodeBucket = new BucketAttributes();
        nodeBucket.put(EXECUTION_ID, reportNode.getExecutionID());
        nodeBucket.put(ARTEFACT_HASH, reportNode.getArtefactHash());
        nodeBucket.put(STATUS, reportNode.getStatus().toString());
        if (reportNode.getError() != null) {
            nodeBucket.put(ERROR_CODE, reportNode.getError().getCode() != null ? reportNode.getError().getCode() : 0);
            if (reportNode.getError().getMsg() != null) {
                nodeBucket.put(ERROR_MESSAGE, reportNode.getError().getMsg());
            }
        }
        if (customAttributes != null) {
            nodeBucket.putAll(customAttributes);
        }
        return nodeBucket;
    }

    public void flush() {
        ingestionPipeline.flush();
    }

    public boolean isIngestionEnabled() {
        return ingestionEnabled;
    }

    public static class Range {
        public long from;
        public long to;

        public Range() {
        }

        public Range(long from, long to) {
            this.from = from;
            this.to = to;
        }
    }


    public Map<String, Map<String, Bucket>> queryByExecutionIdAndGroupBy(String executionId, Range range, String groupLevel1, String groupLevel2) {
        Filter filter = Filters.equals("attributes." + EXECUTION_ID, executionId);
        Set<String> groupBy = Set.of(groupLevel1, groupLevel2);
        TimeSeriesAggregationQueryBuilder queryBuilder = new TimeSeriesAggregationQueryBuilder()
            .withOptimizationType(TimeSeriesOptimizationType.MOST_EFFICIENT)
            .withFilter(filter)
            .withGroupDimensions(groupBy)
            .split(1);

        if (range != null) {
            queryBuilder.range(range.from, range.to);
        }
        return timeSeries.getAggregationPipeline().collect(queryBuilder.build())
            .getSeries().entrySet().stream()
            .filter(e -> e.getKey().keySet().containsAll(groupBy))
            .collect(Collectors.groupingBy(
                e -> (String) e.getKey().get(groupLevel1), // outer key
                Collectors.toMap(
                    e -> (String) e.getKey().get(groupLevel2),    // inner key
                    e -> e.getValue().values().stream().findFirst().orElse(new Bucket())
                )
            ));
    }

    @Override
    public void close() {
        timeSeries.close();
    }
}
