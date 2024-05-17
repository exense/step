package step.core.artefacts.reports.aggregated;

import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.collections.CollectionFactory;
import step.core.collections.Filters;
import step.core.collections.IndexField;
import step.core.collections.Order;
import step.core.collections.filters.And;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.TimeSeriesIngestionPipeline;
import step.core.timeseries.aggregation.TimeSeriesAggregationPipeline;
import step.core.timeseries.aggregation.TimeSeriesAggregationQueryBuilder;
import step.core.timeseries.bucket.BucketAttributes;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ReportNodeTimeSeries implements Closeable {

    public static final String ARTEFACT_HASH = "artefactHash";
    public static final String EXECUTION_ID = "executionId";
    public static final String STATUS = "status";
    private final TimeSeriesIngestionPipeline timeSeriesIngestionPipeline;
    private final TimeSeriesAggregationPipeline aggregationPipeline;

    public ReportNodeTimeSeries(CollectionFactory collectionFactory) {
        TimeSeries reportNodeTimeSeries = new TimeSeries(collectionFactory, "reportNodeTimeSeries", 60000);
        timeSeriesIngestionPipeline = reportNodeTimeSeries.newIngestionPipeline(10);

        aggregationPipeline = reportNodeTimeSeries.getAggregationPipeline();
        reportNodeTimeSeries.createIndexes(Set.of(new IndexField(EXECUTION_ID, Order.ASC, String.class)));
    }

    public void ingestReportNode(ReportNode reportNode) {
        ReportNodeStatus status = reportNode.getStatus();
        timeSeriesIngestionPipeline.ingestPoint(new BucketAttributes(Map.of(EXECUTION_ID, reportNode.getExecutionID(), ARTEFACT_HASH, reportNode.getArtefactHash(), STATUS, status.toString())), System.currentTimeMillis(), 1);
    }

    public static class Range {
        long from;
        long to;
    }

    public Map<String, Long> queryByExecutionIdAndArtefactHash(String executionId, String artefactHash, Range range) {
        And filter = Filters.and(List.of(Filters.equals("attributes." + EXECUTION_ID, executionId), Filters.equals("attributes." + ARTEFACT_HASH, artefactHash)));
        TimeSeriesAggregationQueryBuilder builder = aggregationPipeline.newQueryBuilder().withFilter(filter).withGroupDimensions(Set.of(STATUS)).split(1);
        if (range != null) {
            builder.range(range.from, range.to);
        }
        Map<String, Long> countByStatus = builder.build().run()
                .getSeries().entrySet().stream().collect(Collectors.toMap(k -> (String) k.getKey().get(STATUS), v -> v.getValue().values().stream().findFirst().get().getCount()));
        return countByStatus;
    }

    @Override
    public void close() throws IOException {
        timeSeriesIngestionPipeline.close();
    }
}
