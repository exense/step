package step.plugins.timeseries;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.collections.CollectionFactory;
import step.core.deployment.WebApplicationConfigurationManager;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.TimeSeriesAggregationPipeline;
import step.core.timeseries.TimeSeriesIngestionPipeline;
import step.plugins.measurements.GaugeCollectorRegistry;
import step.plugins.measurements.MeasurementPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Plugin
public class TimeSeriesControllerPlugin extends AbstractControllerPlugin {

    public static String RESOLUTION_PERIOD_PROPERTY = "plugins.timeseries.resolution.period";
    public static String TIME_SERIES_COLLECTION_PROPERTY = "timeseries";

    public static String TIME_SERIES_ATTRIBUTES_PROPERTY = "plugins.timeseries.attributes";
    public static String TIME_SERIES_ATTRIBUTES_DEFAULT = "eId,taskId,metricType,name,rnStatus,project,type";

    private static final Logger logger = LoggerFactory.getLogger(TimeSeriesControllerPlugin.class);
    private TimeSeriesIngestionPipeline mainIngestionPipeline;

    @Override
    public void serverStart(GlobalContext context) {
        Configuration configuration = context.getConfiguration();
        Integer resolutionPeriod = configuration.getPropertyAsInteger(RESOLUTION_PERIOD_PROPERTY, 1000);
        Long flushPeriod = configuration.getPropertyAsLong("plugins.timeseries.flush.period", 1000L);
        List<String> attributes = Arrays.asList(configuration.getProperty(TIME_SERIES_ATTRIBUTES_PROPERTY, TIME_SERIES_ATTRIBUTES_DEFAULT).split(","));
        CollectionFactory collectionFactory = context.getCollectionFactory();

        TimeSeries timeSeries = new TimeSeries(collectionFactory, TIME_SERIES_COLLECTION_PROPERTY, Set.of(), resolutionPeriod);
        context.put(TimeSeries.class, timeSeries);
        mainIngestionPipeline = timeSeries.newIngestionPipeline(flushPeriod);
        TimeSeriesAggregationPipeline aggregationPipeline = timeSeries.getAggregationPipeline();

        context.put(TimeSeriesIngestionPipeline.class, mainIngestionPipeline);
        context.put(TimeSeriesAggregationPipeline.class, aggregationPipeline);

        context.getServiceRegistrationCallback().registerService(TimeSeriesService.class);
        TimeSeriesBucketingHandler handler = new TimeSeriesBucketingHandler(mainIngestionPipeline, attributes);
        MeasurementPlugin.registerMeasurementHandlers(handler);
        GaugeCollectorRegistry.getInstance().registerHandler(handler);

        WebApplicationConfigurationManager configurationManager = context.require(WebApplicationConfigurationManager.class);
        configurationManager.registerHook(s -> Map.of(RESOLUTION_PERIOD_PROPERTY, resolutionPeriod.toString()));

    }

    @Override
    public void serverStop(GlobalContext context) {
        mainIngestionPipeline.close();
    }
}
