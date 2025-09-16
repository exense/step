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
import step.core.timeseries.ingestion.TimeSeriesIngestionPipeline;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        this(collectionFactory, TimeSeriesCollectionsSettings.readSettings(configuration, TIME_SERIES_MAIN_COLLECTION),
                configuration.getPropertyAsBoolean(CONF_KEY_REPORT_NODE_TIME_SERIES_ENABLED, true));
    }

    public ReportNodeTimeSeries(CollectionFactory collectionFactory, TimeSeriesCollectionsSettings timeSeriesCollectionsSettings, boolean ingestionEnabled) {
        List<TimeSeriesCollection> timeSeriesCollections = getTimeSeriesCollections(timeSeriesCollectionsSettings, collectionFactory);
        timeSeries = new TimeSeriesBuilder().registerCollections(timeSeriesCollections).build();
        ingestionPipeline = timeSeries.getIngestionPipeline();
        timeSeries.createIndexes(Set.of(new IndexField(EXECUTION_ID, Order.ASC, String.class)));
        this.ingestionEnabled = ingestionEnabled;
    }

    private List<TimeSeriesCollection> getTimeSeriesCollections(TimeSeriesCollectionsSettings collectionsSettings, CollectionFactory collectionFactory) {
        TimeSeriesCollectionsBuilder timeSeriesCollectionsBuilder = new TimeSeriesCollectionsBuilder(collectionFactory);
        return timeSeriesCollectionsBuilder.getTimeSeriesCollections(TIME_SERIES_MAIN_COLLECTION, collectionsSettings, Set.of(EXECUTION_ID));
    }

    public TimeSeries getTimeSeries() {
        return timeSeries;
    }

    public void ingestReportNode(ReportNode reportNode) {
        ingestReportNode(reportNode, null);
    }

    public void ingestReportNode(ReportNode reportNode, Map<String, Object> customAttributes) {
        ReportNodeStatus status = reportNode.getStatus();
        BucketAttributes nodeBucket = new BucketAttributes();
        nodeBucket.put(EXECUTION_ID, reportNode.getExecutionID());
        nodeBucket.put(ARTEFACT_HASH, reportNode.getArtefactHash());
        nodeBucket.put(STATUS, status.toString());
        if (reportNode.getError() != null) {
            nodeBucket.put(ERROR_CODE, reportNode.getError().getCode());
            nodeBucket.put(ERROR_MESSAGE, reportNode.getError().getMsg());
        }
        if (customAttributes != null) {
            nodeBucket.putAll(customAttributes);
        }

        ingestionPipeline.ingestPoint(new BucketAttributes(nodeBucket), reportNode.getExecutionTime(), reportNode.getDuration());
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
    }


    public Map<String, Map<String, Bucket>> queryByExecutionIdAndGroupBy(String executionId, Range range, String groupLevel1, String groupLevel2) {
        Filter filter = Filters.equals("attributes." + EXECUTION_ID, executionId);
        Set<String> groupBy = Set.of(groupLevel1, groupLevel2);
        TimeSeriesAggregationQueryBuilder queryBuilder = new TimeSeriesAggregationQueryBuilder()
                .withOptimizationType(TimeSeriesOptimizationType.MOST_ACCURATE)
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
                                e -> (String)  e.getKey().get(groupLevel2),    // inner key
                                e -> e.getValue().values().stream().findFirst().orElse(new Bucket())
                        )
                ));
    }

    @Override
    public void close() {
        timeSeries.close();
    }
}
