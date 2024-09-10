package step.plugins.timeseries.collections;

import step.core.collections.CollectionFactory;
import step.core.timeseries.TimeSeriesCollection;
import step.core.timeseries.bucket.Bucket;
import step.plugins.timeseries.TimeSeriesControllerPlugin;

import java.util.concurrent.TimeUnit;

public class HourlyTimeSeriesCollection extends TimeSeriesCollection {
    
    public HourlyTimeSeriesCollection(CollectionFactory collectionFactory) {
        super(collectionFactory.getCollection(TimeSeriesControllerPlugin.TIME_SERIES_HOURLY_COLLECTION, Bucket.class), TimeUnit.HOURS.toMillis(1));
    }
}
