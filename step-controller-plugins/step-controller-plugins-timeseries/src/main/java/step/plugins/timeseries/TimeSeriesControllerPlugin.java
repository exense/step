package step.plugins.timeseries;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.collections.CollectionFactory;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.TimeSeriesAggregationPipeline;
import step.core.timeseries.TimeSeriesIngestionPipeline;
import step.plugins.measurements.GaugeCollectorRegistry;
import step.plugins.measurements.MeasurementPlugin;

import java.util.Set;

@Plugin
public class TimeSeriesControllerPlugin extends AbstractControllerPlugin {

    public static String RESOLUTION_PERIOD_PROPERTY = "plugins.timeseries.resolution.period";
    public static String TIME_SERIES_COLLECTION_PROPERTY = "timeseries";

    private static final Logger logger = LoggerFactory.getLogger(TimeSeriesControllerPlugin.class);
    private TimeSeriesIngestionPipeline ingestionPipeline;

    @Override
    public void serverStart(GlobalContext context) {
        Configuration configuration = context.getConfiguration();
        Integer resolutionPeriod = configuration.getPropertyAsInteger(RESOLUTION_PERIOD_PROPERTY, 1000);
        Long flushPeriod = configuration.getPropertyAsLong("plugins.timeseries.flush.period", 1000L);
        CollectionFactory collectionFactory = context.getCollectionFactory();

        TimeSeries timeSeries = new TimeSeries(collectionFactory, TIME_SERIES_COLLECTION_PROPERTY, Set.of());

        ingestionPipeline = timeSeries.newIngestionPipeline(resolutionPeriod, flushPeriod);
        TimeSeriesAggregationPipeline aggregationPipeline = timeSeries.getAggregationPipeline(resolutionPeriod);

        context.put(TimeSeriesIngestionPipeline.class, ingestionPipeline);
        context.put(TimeSeriesAggregationPipeline.class, aggregationPipeline);

        context.getServiceRegistrationCallback().registerService(TimeSeriesService.class);
        TimeSeriesBucketingHandler handler = new TimeSeriesBucketingHandler(ingestionPipeline);
        MeasurementPlugin.registerMeasurementHandlers(handler);
        GaugeCollectorRegistry.getInstance().registerHandler(handler);

    }

    @Override
    public void serverStop(GlobalContext context) {
        ingestionPipeline.close();
    }
}
