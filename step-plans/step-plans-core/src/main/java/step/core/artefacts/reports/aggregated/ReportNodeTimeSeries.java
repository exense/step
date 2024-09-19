package step.core.artefacts.reports.aggregated;

import ch.exense.commons.app.Configuration;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.collections.*;
import step.core.collections.filters.And;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.TimeSeriesBuilder;
import step.core.timeseries.TimeSeriesCollection;
import step.core.timeseries.TimeSeriesCollectionSettings;
import step.core.timeseries.aggregation.TimeSeriesAggregationQueryBuilder;
import step.core.timeseries.bucket.Bucket;
import step.core.timeseries.bucket.BucketAttributes;
import step.core.timeseries.ingestion.TimeSeriesIngestionPipeline;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ReportNodeTimeSeries implements Closeable {

    public static final String TIME_SERIES_MAIN_COLLECTION_FLUSH_PERIOD = "plugins.timeseries.collections.main.flush.period";
    public static final String TIME_SERIES_MINUTE_COLLECTION_ENABLED = "plugins.timeseries.collections.minute.enabled";
    public static final String TIME_SERIES_MINUTE_COLLECTION_FLUSH_PERIOD = "plugins.timeseries.collections.minute.flush.period";
    public static final String TIME_SERIES_HOUR_COLLECTION_ENABLED = "plugins.timeseries.collections.hour.enabled";
    public static final String TIME_SERIES_HOUR_COLLECTION_FLUSH_PERIOD = "plugins.timeseries.collections.hour.flush.period";
    public static final String TIME_SERIES_DAY_COLLECTION_ENABLED = "plugins.timeseries.collections.day.enabled";
    public static final String TIME_SERIES_DAY_COLLECTION_FLUSH_PERIOD = "plugins.timeseries.collections.day.flush.period";
    public static final String TIME_SERIES_WEEK_COLLECTION_ENABLED = "plugins.timeseries.collections.week.enabled";
    public static final String TIME_SERIES_WEEK_COLLECTION_FLUSH_PERIOD = "plugins.timeseries.collections.week.flush.period";
    public static final String TIME_SERIES_MAIN_COLLECTION = "reportNodeTimeSeries";
    public static final String TIME_SERIES_PER_MINUTE_COLLECTION = "reportNodeTimeSeries_minute";
    public static final String TIME_SERIES_HOURLY_COLLECTION = "reportNodeTimeSeries_hour";
    public static final String TIME_SERIES_DAILY_COLLECTION = "reportNodeTimeSeries_day";
    public static final String TIME_SERIES_WEEKLY_COLLECTION = "reportNodeTimeSeries_week";

    public static final String ARTEFACT_HASH = "artefactHash";
    public static final String EXECUTION_ID = "executionId";
    public static final String STATUS = "status";
    private final TimeSeries timeSeries;
    private final TimeSeriesIngestionPipeline ingestionPipeline;

    public ReportNodeTimeSeries(CollectionFactory collectionFactory, Configuration configuration) {
        ReportNodeTimeSeriesCollectionsSettings collectionsSettings = getCollectionsSettings(configuration);
        timeSeries = new TimeSeriesBuilder()
                .registerCollections(getTimeSeriesCollections(collectionsSettings, collectionFactory))
                .build();
        ingestionPipeline = timeSeries.getIngestionPipeline();
        timeSeries.createIndexes(Set.of(new IndexField(EXECUTION_ID, Order.ASC, String.class)));
    }

    private List<TimeSeriesCollection> getTimeSeriesCollections(ReportNodeTimeSeriesCollectionsSettings collectionsSettings, CollectionFactory collectionFactory) {
        HashMap<TimeSeriesCollection, Boolean> collectionsEnabled = new HashMap<>();

        collectionsEnabled.put(
                new TimeSeriesCollection(collectionFactory.getCollection(TIME_SERIES_MAIN_COLLECTION, Bucket.class),
                        new TimeSeriesCollectionSettings()
                                .setResolution(30_000)
                                .setIngestionFlushingPeriodMs(collectionsSettings.getMainFlushInterval())
                ), true);
        collectionsEnabled.put(
                new TimeSeriesCollection(collectionFactory.getCollection(TIME_SERIES_PER_MINUTE_COLLECTION, Bucket.class),
                        new TimeSeriesCollectionSettings()
                                .setResolution(TimeUnit.MINUTES.toMillis(1))
                                .setIngestionFlushingPeriodMs(collectionsSettings.getPerMinuteFlushInterval())
                                .setMergeBucketsOnIngestionFlush(true))
                , collectionsSettings.isPerMinuteEnabled());
        collectionsEnabled.put(
                new TimeSeriesCollection(collectionFactory.getCollection(TIME_SERIES_HOURLY_COLLECTION, Bucket.class),
                        new TimeSeriesCollectionSettings()
                                .setResolution(TimeUnit.HOURS.toMillis(1))
                                .setIngestionFlushingPeriodMs(collectionsSettings.getHourlyFlushInterval())
                                .setMergeBucketsOnIngestionFlush(true))
                , collectionsSettings.isPerMinuteEnabled());
        collectionsEnabled.put(
                new TimeSeriesCollection(collectionFactory.getCollection(TIME_SERIES_DAILY_COLLECTION, Bucket.class),
                        new TimeSeriesCollectionSettings()
                                .setResolution(TimeUnit.DAYS.toMillis(1))
                                .setIngestionFlushingPeriodMs(collectionsSettings.getDailyFlushInterval())
                                .setMergeBucketsOnIngestionFlush(true))
                , collectionsSettings.isPerMinuteEnabled());
        collectionsEnabled.put(
                new TimeSeriesCollection(collectionFactory.getCollection(TIME_SERIES_WEEKLY_COLLECTION, Bucket.class),
                        new TimeSeriesCollectionSettings()
                                .setResolution(TimeUnit.DAYS.toMillis(7))
                                .setIngestionFlushingPeriodMs(collectionsSettings.getWeeklyFlushInterval())
                                .setMergeBucketsOnIngestionFlush(true))
                , collectionsSettings.isPerMinuteEnabled());
        List<TimeSeriesCollection> enabledCollections = new ArrayList<>();

        collectionsEnabled.forEach((collection, enabled) -> {
            if (enabled) {
                enabledCollections.add(collection);
            } else {
                // disabled resolutions will be completely dropped from db
                collection.getCollection().drop();
            }
        });
        return enabledCollections;
    }

    public TimeSeries getTimeSeries() {
        return timeSeries;
    }

    private static ReportNodeTimeSeriesCollectionsSettings getCollectionsSettings(Configuration configuration) {
        return new ReportNodeTimeSeriesCollectionsSettings()
                .setMainFlushInterval(configuration.getPropertyAsLong(TIME_SERIES_MAIN_COLLECTION_FLUSH_PERIOD, TimeUnit.SECONDS.toMillis(1)))
                .setPerMinuteEnabled(configuration.getPropertyAsBoolean(TIME_SERIES_MINUTE_COLLECTION_ENABLED, true))
                .setPerMinuteFlushInterval(configuration.getPropertyAsLong(TIME_SERIES_MINUTE_COLLECTION_FLUSH_PERIOD, TimeUnit.MINUTES.toMillis(1)))
                .setHourlyEnabled(configuration.getPropertyAsBoolean(TIME_SERIES_HOUR_COLLECTION_ENABLED, true))
                .setHourlyFlushInterval(configuration.getPropertyAsLong(TIME_SERIES_HOUR_COLLECTION_FLUSH_PERIOD, TimeUnit.MINUTES.toMillis(5)))
                .setDailyEnabled(configuration.getPropertyAsBoolean(TIME_SERIES_DAY_COLLECTION_ENABLED, true))
                .setDailyFlushInterval(configuration.getPropertyAsLong(TIME_SERIES_DAY_COLLECTION_FLUSH_PERIOD, TimeUnit.HOURS.toMillis(1)))
                .setWeeklyEnabled(configuration.getPropertyAsBoolean(TIME_SERIES_WEEK_COLLECTION_ENABLED, true))
                .setWeeklyFlushInterval(configuration.getPropertyAsLong(TIME_SERIES_WEEK_COLLECTION_FLUSH_PERIOD, TimeUnit.HOURS.toMillis(2)));
    }

    public void ingestReportNode(ReportNode reportNode) {
        ReportNodeStatus status = reportNode.getStatus();
        ingestionPipeline.ingestPoint(new BucketAttributes(Map.of(EXECUTION_ID, reportNode.getExecutionID(), ARTEFACT_HASH, reportNode.getArtefactHash(), STATUS, status.toString())), System.currentTimeMillis(), 1);
    }

    public void flush() {
        ingestionPipeline.flush();
    }

    public static class Range {
        long from;
        long to;
    }

    public Map<String, Long> queryByExecutionIdAndArtefactHash(String executionId, String artefactHash, Range range) {
        And filter = Filters.and(List.of(Filters.equals("attributes." + EXECUTION_ID, executionId), Filters.equals("attributes." + ARTEFACT_HASH, artefactHash)));
        TimeSeriesAggregationQueryBuilder builder = new TimeSeriesAggregationQueryBuilder()
                .withFilter(filter)
                .withGroupDimensions(Set.of(STATUS))
                .split(1);

        if (range != null) {
            builder.range(range.from, range.to);
        }
        Map<String, Long> countByStatus = timeSeries.getAggregationPipeline().collect(builder.build())
                .getSeries().entrySet().stream().collect(Collectors.toMap(k -> (String) k.getKey().get(STATUS), v -> v.getValue().values().stream().findFirst().get().getCount()));
        return countByStatus;
    }

    @Override
    public void close() throws IOException {
        timeSeries.close();
    }
}
