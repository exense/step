package step.core.artefacts.reports.aggregated;

import ch.exense.commons.app.Configuration;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.collections.CollectionFactory;
import step.core.collections.Filters;
import step.core.collections.IndexField;
import step.core.collections.Order;
import step.core.collections.filters.And;
import step.core.timeseries.*;
import step.core.timeseries.aggregation.TimeSeriesAggregationQueryBuilder;
import step.core.timeseries.aggregation.TimeSeriesOptimizationType;
import step.core.timeseries.bucket.BucketAttributes;
import step.core.timeseries.ingestion.TimeSeriesIngestionPipeline;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ReportNodeTimeSeries implements Closeable {

    public static final String TIME_SERIES_MAIN_COLLECTION = "reportNodeTimeSeries";
    public static final String ARTEFACT_HASH = "artefactHash";
    public static final String EXECUTION_ID = "executionId";
    public static final String STATUS = "status";
    private final TimeSeries timeSeries;
    private final TimeSeriesIngestionPipeline ingestionPipeline;

    public ReportNodeTimeSeries(CollectionFactory collectionFactory, Configuration configuration) {
        TimeSeriesCollectionsSettings timeSeriesCollectionsSettings = TimeSeriesCollectionsSettings.readSettings(configuration, TIME_SERIES_MAIN_COLLECTION);
        List<TimeSeriesCollection> timeSeriesCollections = getTimeSeriesCollections(timeSeriesCollectionsSettings, collectionFactory);
        timeSeries = new TimeSeriesBuilder().registerCollections(timeSeriesCollections).build();
        ingestionPipeline = timeSeries.getIngestionPipeline();
        timeSeries.createIndexes(Set.of(new IndexField(EXECUTION_ID, Order.ASC, String.class)));
    }

    private List<TimeSeriesCollection> getTimeSeriesCollections(TimeSeriesCollectionsSettings collectionsSettings, CollectionFactory collectionFactory) {
        TimeSeriesCollectionsBuilder timeSeriesCollectionsBuilder = new TimeSeriesCollectionsBuilder(collectionFactory);
        return timeSeriesCollectionsBuilder.getTimeSeriesCollections(TIME_SERIES_MAIN_COLLECTION, collectionsSettings);
    }

    public TimeSeries getTimeSeries() {
        return timeSeries;
    }

    public void ingestReportNode(ReportNode reportNode) {
        ReportNodeStatus status = reportNode.getStatus();
        ingestionPipeline.ingestPoint(new BucketAttributes(Map.of(EXECUTION_ID, reportNode.getExecutionID(), ARTEFACT_HASH, reportNode.getArtefactHash(), STATUS, status.toString())), System.currentTimeMillis(), 1);
    }

    public void flush() {
        ingestionPipeline.flush();
    }

    public void flush() {
        timeSeriesIngestionPipeline.flush();
    }

    public static class Range {
        long from;
        long to;
    }

    public Map<String, Long> queryByExecutionIdAndArtefactHash(String executionId, String artefactHash, Range range) {
        And filter = Filters.and(List.of(Filters.equals("attributes." + EXECUTION_ID, executionId), Filters.equals("attributes." + ARTEFACT_HASH, artefactHash)));
        TimeSeriesAggregationQueryBuilder queryBuilder = new TimeSeriesAggregationQueryBuilder()
                .withOptimizationType(TimeSeriesOptimizationType.MOST_ACCURATE)
                .withFilter(filter)
                .withGroupDimensions(Set.of(STATUS))
                .split(1);

        if (range != null) {
            queryBuilder.range(range.from, range.to);
        }
        Map<String, Long> countByStatus = timeSeries.getAggregationPipeline().collect(queryBuilder.build())
                .getSeries().entrySet().stream().collect(Collectors.toMap(k -> (String) k.getKey().get(STATUS), v -> v.getValue().values().stream().findFirst().get().getCount()));
        return countByStatus;
    }

    @Override
    public void close() throws IOException {
        timeSeries.close();
    }
}
