package step.plugins.timeseries;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.rtm.commons.MeasurementAccessor;
import step.controller.services.async.AsyncTaskManager;
import step.controller.services.async.AsyncTaskStatus;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Filters;
import step.core.collections.SearchOrder;
import step.core.collections.filters.Equals;
import step.core.deployment.AbstractStepServices;
import step.core.deployment.ControllerServiceException;
import step.core.timeseries.*;
import step.framework.server.security.Secured;
import step.plugins.measurements.Measurement;
import step.plugins.measurements.MeasurementPlugin;
import step.plugins.timeseries.api.*;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static step.plugins.timeseries.TimeSeriesControllerPlugin.RESOLUTION_PERIOD_PROPERTY;
import static step.plugins.timeseries.TimeSeriesControllerPlugin.TIME_SERIES_COLLECTION_PROPERTY;

@Singleton
@Path("/time-series")
@Tag(name = "TimeSeries")
public class TimeSeriesService extends AbstractStepServices {

    private AsyncTaskManager asyncTaskManager;
    private TimeSeriesAggregationPipeline aggregationPipeline;
    private Collection<Measurement> measurementCollection;
    private TimeSeries timeSeries;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        aggregationPipeline = context.require(TimeSeriesAggregationPipeline.class);
        asyncTaskManager = context.require(AsyncTaskManager.class);
        measurementCollection = context.getCollectionFactory().getCollection(MeasurementAccessor.ENTITY_NAME, Measurement.class);
        timeSeries = context.require(TimeSeries.class);
    }

    @Secured(right = "execution-read")
    @POST
    @Path("/buckets")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TimeSeriesAPIResponse getBuckets(FetchBucketsRequest request) {
        TimeSeriesAggregationQuery query = mapToQuery(request);
        TimeSeriesAggregationResponse response = query.run();
        return new TimeSeriesAPIResponseBuilder()
                .withStart(response.getStart())
                .withEnd(response.getEnd())
                .withInterval(response.getInterval())
                .withMatrixKeys(response.getMatrixKeys())
                .withMatrix(response.getMatrix()
                        .stream()
                        .map(buckets ->
                                Stream.of(buckets)
                                        .map(b -> b == null ? null : new BucketResponseBuilder()
                                                .withBegin(b.getBegin())
                                                .withCount(b.getCount())
                                                .withMin(b.getMin())
                                                .withMax(b.getMax())
                                                .withSum(b.getSum())
                                                .withThroughputPerHour(3600 * 1000 * b.getCount() / query.getIntervalSizeMs())
                                                .withPclValues(request.getPercentiles().stream().collect(Collectors.toMap(p -> p, b::getPercentile)))
                                                .build())
                                        .toArray(BucketResponse[]::new))
                        .collect(Collectors.toList()))
                .build();
    }

    private TimeSeriesAggregationQuery mapToQuery(FetchBucketsRequest request) {
        return aggregationPipeline.newQuery()
                .range(request.getStart(), request.getEnd())
                .window(request.getIntervalSize())
                .filter(request.getParams() != null ? request.getParams() : Collections.emptyMap())
                .split(request.getNumberOfBuckets())
                .groupBy(request.getGroupDimensions());
    }

    @Operation(operationId = "rebuildTimeSeries", description = "Rebuild a time series based on the provided request")
    @Secured(right = "execution-read")
    @POST
    @Path("/rebuild")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AsyncTaskStatus<Object> rebuildTimeSeries(TimeSeriesRebuildRequest request) {
        String executionId = Objects.requireNonNull(request.getExecutionId(), "executionId not specified");
        if (this.timeSeriesExists(executionId)) {
            throw new ControllerServiceException("Time series already exist for this execution. Unable to rebuild it");
        } else {
            // we need to check if measurements exists
            Equals measurementFilter = Filters.equals(MeasurementPlugin.ATTRIBUTE_EXECUTION_ID, executionId);
            Measurement firstMeasurement = measurementCollection.find(measurementFilter,
                    new SearchOrder(MeasurementPlugin.BEGIN, 1), 0, 1, 0).findFirst().orElse(null);
            Measurement lastMeasurement = measurementCollection.find(measurementFilter,
                    new SearchOrder(MeasurementPlugin.BEGIN, -1), 0, 1, 0).findFirst().orElse(null);
            if (firstMeasurement != null && lastMeasurement != null) {
                return asyncTaskManager.scheduleAsyncTask(t -> {
                    // the flushing period can be a big value, because we will force flush every time.
                    // we create a new pipeline for every migration
                    try (TimeSeriesIngestionPipeline ingestionPipeline = timeSeries.newIngestionPipeline( 3000)) {
                        TimeSeriesBucketingHandler timeSeriesBucketingHandler = new TimeSeriesBucketingHandler(ingestionPipeline);
                        LongAdder count = new LongAdder();
                        SearchOrder searchOrder = new SearchOrder("begin", 1);
                        // Iterate over each measurement and ingest it again
                        measurementCollection.find(measurementFilter, searchOrder, null, null, 0).forEach(measurement -> {
                            count.increment();
                            timeSeriesBucketingHandler.ingestExistingMeasurement(measurement);
                        });
                        ingestionPipeline.flush();
                        return new TimeSeriesRebuildResponse(count.longValue());
                    }
                });
            } else {
                throw new ControllerServiceException("No measurement found matching this execution id");
            }
        }
    }

    @Operation(operationId = "checkTimeSeries", description = "Check if the time-series was created for a specific execution")
    @Secured(right = "execution-read")
    @GET
    @Path("/execution/{executionId}/exists")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public boolean timeSeriesIsBuilt(@PathParam("executionId") String executionId) {
        return timeSeriesExists(executionId);
    }

    private boolean timeSeriesExists(String executionId) {
        TimeSeriesAggregationResponse aggregationResponse = aggregationPipeline.newQuery().range(0, System.currentTimeMillis())
                .filter(Map.of(MeasurementPlugin.ATTRIBUTE_EXECUTION_ID, executionId)).split(1L).run();
        return aggregationResponse.getMatrix().size() > 0;
    }
}
