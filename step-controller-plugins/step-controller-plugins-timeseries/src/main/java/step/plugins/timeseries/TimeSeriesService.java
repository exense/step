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
import step.core.collections.Document;
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

import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import static step.plugins.timeseries.TimeSeriesControllerPlugin.TIME_SERIES_COLLECTION_PROPERTY;

@Singleton
@Path("/time-series")
@Tag(name = "TimeSeries")
public class TimeSeriesService extends AbstractStepServices {

    private AsyncTaskManager asyncTaskManager;
    private TimeSeriesAggregationPipeline aggregationPipeline;
    private Collection<Measurement> measurementCollection;
    private TimeSeries timeSeries;

    private Collection<Document> timeserieCollection;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        aggregationPipeline = context.require(TimeSeriesAggregationPipeline.class);
        asyncTaskManager = context.require(AsyncTaskManager.class);
        measurementCollection = context.getCollectionFactory().getCollection(MeasurementAccessor.ENTITY_NAME, Measurement.class);
        timeserieCollection = context.getCollectionFactory().getCollection(TIME_SERIES_COLLECTION_PROPERTY, Document.class);
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

        Map<BucketAttributes, Map<Long, Bucket>> series = response.getSeries();
        long intervalSize = response.getResolution();
        List<Long> axis = response.getAxis();
        Long start = axis.get(0);
        Long end = axis.get(axis.size() - 1) + intervalSize;

        List<BucketAttributes> matrixKeys = new ArrayList<>();
        List<List<BucketResponse>> matrix = new ArrayList<>();
        series.keySet().forEach(key -> {
            Map<Long, Bucket> currentSeries = series.get(key);
            List<BucketResponse> bucketResponses = new ArrayList<>();
            axis.forEach(index -> {
                Bucket b = currentSeries.get(index);
                BucketResponse bucketResponse = null;
                if (b != null) {
                    bucketResponse = new BucketResponseBuilder()
                            .withBegin(b.getBegin())
                            .withCount(b.getCount())
                            .withMin(b.getMin())
                            .withMax(b.getMax())
                            .withSum(b.getSum())
                            .withThroughputPerHour(3600 * 1000 * b.getCount() / (b.getEnd() - b.getBegin()))
                            .withPclValues(request.getPercentiles().stream().collect(Collectors.toMap(p -> p, b::getPercentile)))
                            .build();
                }
                bucketResponses.add(bucketResponse);
            });
            matrix.add(bucketResponses);
            matrixKeys.add(key);
        });

        return new TimeSeriesAPIResponseBuilder()
                .withStart(start)
                .withEnd(end)
                .withInterval(intervalSize)
                .withMatrixKeys(matrixKeys)
                .withMatrix(matrix)
                .build();
    }

    private TimeSeriesAggregationQuery mapToQuery(FetchBucketsRequest request) {
        TimeSeriesAggregationQuery timeSeriesAggregationQuery = aggregationPipeline.newQuery()
                .range(request.getStart(), request.getEnd())
                .filter(request.getParams() != null ? request.getParams() : Collections.emptyMap())
                .groupBy(request.getGroupDimensions());
        if (request.getIntervalSize() > 0) {
            timeSeriesAggregationQuery.window(request.getIntervalSize());
        }
        if (request.getNumberOfBuckets() != null) {
            timeSeriesAggregationQuery.split(request.getNumberOfBuckets());
        }
        return timeSeriesAggregationQuery;
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
                    try (TimeSeriesIngestionPipeline ingestionPipeline = timeSeries.newIngestionPipeline(3000)) {
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
        return timeSeriesExists(executionId) || !hasRawMeasurements(executionId);
    }

    private boolean hasRawMeasurements(String executionId) {
        Measurement measurement = measurementCollection.find(Filters.equals(MeasurementPlugin.ATTRIBUTE_EXECUTION_ID, executionId), null, null, 1, 0).findFirst().orElse(null);
        return measurement != null;
    }


    private boolean timeSeriesExists(String executionId) {
        Document document = timeserieCollection.find(Filters.equals("attributes." + MeasurementPlugin.ATTRIBUTE_EXECUTION_ID, executionId), null, null, 1, 0).findFirst().orElse(null);
        return document != null;
    }
}
