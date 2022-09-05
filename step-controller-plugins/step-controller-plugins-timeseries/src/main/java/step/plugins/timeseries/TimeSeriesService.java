package step.plugins.timeseries;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.rtm.commons.MeasurementAccessor;
import step.controller.services.async.AsyncTaskManager;
import step.controller.services.async.AsyncTaskStatus;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.collections.SearchOrder;
import step.core.deployment.AbstractStepServices;
import step.core.deployment.ControllerServiceException;
import step.core.timeseries.TimeSeriesAggregationPipeline;
import step.core.timeseries.TimeSeriesAggregationQuery;
import step.core.timeseries.TimeSeriesAggregationResponse;
import step.core.timeseries.TimeSeriesIngestionPipeline;
import step.framework.server.security.Secured;
import step.plugins.measurements.Measurement;
import step.plugins.measurements.MeasurementPlugin;
import step.plugins.timeseries.api.*;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@Path("/time-series")
@Tag(name = "TimeSeries")
public class TimeSeriesService extends AbstractStepServices {

    private AsyncTaskManager asyncTaskManager;
    private TimeSeriesAggregationPipeline aggregationPipeline;
    private TimeSeriesIngestionPipeline ingestionPipeline;
    private Collection<Measurement> measurementCollection;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        aggregationPipeline = context.require(TimeSeriesAggregationPipeline.class);
        ingestionPipeline = context.require(TimeSeriesIngestionPipeline.class);
        asyncTaskManager = context.require(AsyncTaskManager.class);
        measurementCollection = context.getCollectionFactory().getCollection(MeasurementAccessor.ENTITY_NAME, Measurement.class);
    }

    @Secured(right = "execution-read")
    @POST
    @Path("/buckets")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TimeSeriesAPIResponse getBucketsNew(FetchBucketsRequest request) {
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
        Filters.Equals measurementFilter = Filters.equals(MeasurementPlugin.ATTRIBUTE_EXECUTION_ID, executionId);
        Measurement firstMeasurement = measurementCollection.find(measurementFilter,
                new SearchOrder(MeasurementPlugin.BEGIN, 1), 0, 1, 0).findFirst().orElse(null);
        Measurement lastMeasurement = measurementCollection.find(measurementFilter,
                new SearchOrder(MeasurementPlugin.BEGIN, -1), 0, 1, 0).findFirst().orElse(null);
        if (firstMeasurement != null && lastMeasurement != null) {
            // Check if a time series already exist for this execution
            TimeSeriesAggregationResponse aggregationResponse = aggregationPipeline.newQuery().range(firstMeasurement.getBegin(), lastMeasurement.getBegin())
                    .filter(Map.of(MeasurementPlugin.ATTRIBUTE_EXECUTION_ID, executionId)).split(1l).run();
            long seriesCount = aggregationResponse.getMatrix().size();
            if (seriesCount > 0) {
                throw new ControllerServiceException("Time series already exist for this execution. Unable to rebuild it");
            } else {
                return asyncTaskManager.scheduleAsyncTask(t -> {
                    TimeSeriesBucketingHandler timeSeriesBucketingHandler = new TimeSeriesBucketingHandler(ingestionPipeline);
                    LongAdder count = new LongAdder();
                    // Iterate over each measurement and ingest it again
                    measurementCollection.find(measurementFilter, null, null, null, 0).forEach(measurement -> {
                        count.increment();
                        timeSeriesBucketingHandler.processMeasurement(measurement);
                    });
                    return new TimeSeriesRebuildResponse(count.longValue());
                });
            }
        } else {
            throw new ControllerServiceException("No measurement found matching this execution id");
        }
    }
}
