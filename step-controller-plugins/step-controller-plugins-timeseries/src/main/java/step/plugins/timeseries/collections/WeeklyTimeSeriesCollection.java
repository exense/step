package step.plugins.timeseries.collections;

import step.core.collections.CollectionFactory;
import step.core.timeseries.TimeSeriesCollection;
import step.core.timeseries.bucket.Bucket;
import step.plugins.timeseries.TimeSeriesControllerPlugin;

import java.util.concurrent.TimeUnit;

public class WeeklyTimeSeriesCollection extends TimeSeriesCollection {
    
    public WeeklyTimeSeriesCollection(CollectionFactory collectionFactory) {
        super(collectionFactory.getCollection(TimeSeriesControllerPlugin.TIME_SERIES_WEEKLY_COLLECTION, Bucket.class), 7 * TimeUnit.DAYS.toMillis(1));
    }
    
}
