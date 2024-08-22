package step.plugins.timeseries.collections;

import step.core.collections.CollectionFactory;
import step.core.timeseries.TimeSeriesCollection;
import step.core.timeseries.bucket.Bucket;
import step.plugins.timeseries.TimeSeriesControllerPlugin;

public class PerMinuteTimeSeriesCollection extends TimeSeriesCollection {
    
    public PerMinuteTimeSeriesCollection(CollectionFactory collectionFactory) {
        // here we can pass custom ingestion pipeline depending on the collection
        super(collectionFactory.getCollection(TimeSeriesControllerPlugin.TIME_SERIES_PER_MINUTE_COLLECTION, Bucket.class), 1000 * 60);
    }
}
