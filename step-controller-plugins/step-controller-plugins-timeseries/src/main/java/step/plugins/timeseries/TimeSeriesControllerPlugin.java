package step.plugins.timeseries;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.timeseries.Bucket;
import step.core.timeseries.TimeSeriesIngestionPipeline;
import step.core.timeseries.accessor.BucketAccessor;
import step.core.timeseries.accessor.BucketAccessorImpl;
import step.plugins.measurements.MeasurementPlugin;

@Plugin
public class TimeSeriesControllerPlugin extends AbstractControllerPlugin {

    private static final Logger logger = LoggerFactory.getLogger(TimeSeriesControllerPlugin.class);

    @Override
    public void executionControllerStart(GlobalContext context) throws Exception {
        Configuration configuration = context.getConfiguration();
        Long resolutionPeriod = configuration.getPropertyAsLong("plugins.timeseries.resolution.period", 100L);
        Long flushPeriod = configuration.getPropertyAsLong("plugins.timeseries.flush.period", 1000L);
        BucketAccessorImpl bucketAccessor = new BucketAccessorImpl(context.getCollectionFactory().getCollection(BucketAccessor.ENTITY_NAME, Bucket.class));
        TimeSeriesIngestionPipeline ingestionPipeline = new TimeSeriesIngestionPipeline(bucketAccessor, resolutionPeriod, flushPeriod);

        context.put(TimeSeriesIngestionPipeline.class, ingestionPipeline);
        context.put(BucketAccessor.class, bucketAccessor);

        MeasurementPlugin.registerMeasurementHandlers(new TimeSeriesBucketingHandler(ingestionPipeline));
    }

}
