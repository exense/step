package step.plugins.timeseries.collections;

import step.core.collections.CollectionFactory;
import step.core.timeseries.TimeSeriesCollection;
import step.core.timeseries.bucket.Bucket;
import step.plugins.timeseries.TimeSeriesControllerPlugin;

public class DailyTimeSeriesCollection extends TimeSeriesCollection {
    
    public DailyTimeSeriesCollection(CollectionFactory collectionFactory) {
        super(collectionFactory.getCollection(TimeSeriesControllerPlugin.TIME_SERIES_DAILY_COLLECTION, Bucket.class), 1000 * 60 * 60 * 24);
    }
}
