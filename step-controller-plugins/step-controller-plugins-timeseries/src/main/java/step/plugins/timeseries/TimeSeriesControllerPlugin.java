package step.plugins.timeseries;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.collections.CollectionFactory;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.timeseries.BucketService;
import step.core.timeseries.TimeSeriesIngestionPipeline;
import step.plugins.measurements.MeasurementPlugin;

@Plugin
public class TimeSeriesControllerPlugin extends AbstractControllerPlugin {

    private static final Logger logger = LoggerFactory.getLogger(TimeSeriesControllerPlugin.class);

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        Configuration configuration = context.getConfiguration();
        Integer resolutionPeriod = configuration.getPropertyAsInteger("plugins.timeseries.resolution.period", 1000);
        Long flushPeriod = configuration.getPropertyAsLong("plugins.timeseries.flush.period", 1000L);
        CollectionFactory collectionFactory = context.getCollectionFactory();
        BucketService bucketService = new BucketService(collectionFactory, resolutionPeriod);
        TimeSeriesIngestionPipeline ingestionPipeline = new TimeSeriesIngestionPipeline(collectionFactory, resolutionPeriod, flushPeriod);
        context.put(TimeSeriesIngestionPipeline.class, ingestionPipeline);
        context.put(BucketService.class, bucketService);

        context.getServiceRegistrationCallback().registerService(TimeSeriesService.class);
        MeasurementPlugin.registerMeasurementHandlers(new TimeSeriesBucketingHandler(ingestionPipeline));
    }

}
