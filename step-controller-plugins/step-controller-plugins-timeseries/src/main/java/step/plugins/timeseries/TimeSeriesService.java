package step.plugins.timeseries;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.apache.commons.lang3.StringUtils;
import org.rtm.commons.MeasurementAccessor;
import step.controller.services.async.AsyncTaskManager;
import step.controller.services.async.AsyncTaskStatus;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.collections.SearchOrder;
import step.core.collections.filters.Equals;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.deployment.AbstractStepServices;
import step.core.deployment.ControllerServiceException;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.ql.OQLFilterBuilder;
import step.core.ql.OQLLexer;
import step.core.timeseries.*;
import step.core.timeseries.aggregation.TimeSeriesAggregationPipeline;
import step.core.timeseries.aggregation.TimeSeriesAggregationQuery;
import step.core.timeseries.aggregation.TimeSeriesAggregationQueryBuilder;
import step.core.timeseries.aggregation.TimeSeriesAggregationResponse;
import step.core.timeseries.bucket.Bucket;
import step.core.timeseries.bucket.BucketAttributes;
import step.core.timeseries.query.OQLTimeSeriesFilterBuilder;
import step.framework.server.security.Secured;
import step.plugins.measurements.Measurement;
import step.plugins.measurements.MeasurementPlugin;
import step.plugins.timeseries.api.*;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;

import static step.plugins.timeseries.TimeSeriesControllerPlugin.TIME_SERIES_ATTRIBUTES_DEFAULT;
import static step.plugins.timeseries.TimeSeriesControllerPlugin.TIME_SERIES_ATTRIBUTES_PROPERTY;
import static step.plugins.timeseries.TimeSeriesExecutionPlugin.TIMESERIES_FLAG;

@Singleton
@Path("/time-series")
@Tag(name = "TimeSeries")
public class TimeSeriesService extends AbstractStepServices {

    private AsyncTaskManager asyncTaskManager;
    private TimeSeriesAggregationPipeline aggregationPipeline;
    private Collection<Measurement> measurementCollection;
    private TimeSeries timeSeries;

    private ExecutionAccessor executionAccessor;
    private List<String> timeSeriesAttributes;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        timeSeriesAttributes = context.get(TimeSeriesBucketingHandler.class).getHandledAttributes();
        aggregationPipeline = context.require(TimeSeriesAggregationPipeline.class);
        asyncTaskManager = context.require(AsyncTaskManager.class);
        measurementCollection = context.getCollectionFactory().getCollection(MeasurementAccessor.ENTITY_NAME, Measurement.class);
        timeSeries = context.require(TimeSeries.class);
        executionAccessor = context.getExecutionAccessor();
    }

    @Secured(right = "execution-read")
    @POST
    @Path("/buckets")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TimeSeriesAPIResponse getBuckets(FetchBucketsRequest request) {
        validateFetchRequest(request);

        OQLVerifyResponse oqlVerifyResponse = this.verifyOql(request.getOqlFilter());
        if (!oqlVerifyResponse.isValid()) {
            throw new ControllerServiceException("Invalid OQL filter");
        }
        if (oqlVerifyResponse.hasUnknownFields() || !this.timeSeriesAttributes.containsAll(request.getGroupDimensions())) {
            return this.getMeasurements(request, oqlVerifyResponse.getFields());
        }
        TimeSeriesAggregationQuery query = mapToQuery(request, this.aggregationPipeline);
        TimeSeriesAggregationResponse response = query.run();
        return mapToApiResponse(request, response);
    }

    public TimeSeriesAPIResponse getMeasurements(FetchBucketsRequest request, java.util.Collection<String> fields) {
        Collection<Bucket> inmemoryBuckets = new InMemoryCollection<>();
        TimeSeries timeSeries = new TimeSeries(inmemoryBuckets, Set.of(), 1000);
        List<String> ignoreFilterAttributes = Arrays.asList("metricType");
        Function<String, String> attributesPrefixRemoval = (attribute) -> {
            if (attribute.startsWith("attributes.")) {
                return attribute.replaceFirst("attributes.", "");
            } else {
                return attribute;
            }
        };
        try (TimeSeriesIngestionPipeline ingestionPipeline = timeSeries.newIngestionPipeline(3000)) {
            List<String> standardAttributes = new ArrayList<>(Arrays.asList(configuration.getProperty(TIME_SERIES_ATTRIBUTES_PROPERTY, TIME_SERIES_ATTRIBUTES_DEFAULT).split(",")));
            standardAttributes.addAll(fields.stream().map(attributesPrefixRemoval).collect(Collectors.toList()));
            standardAttributes.addAll(request.getGroupDimensions());
            TimeSeriesBucketingHandler timeSeriesBucketingHandler = new TimeSeriesBucketingHandler(ingestionPipeline, standardAttributes);
            LongAdder count = new LongAdder();
            ArrayList<Filter> timestampClauses = new ArrayList<>(List.of(Filters.empty()));
            if (request.getStart() != null) {
                timestampClauses.add(Filters.gte("begin", request.getStart()));
            }
            if (request.getEnd() != null) {
                timestampClauses.add(Filters.lt("begin", request.getEnd()));
            }
            Filter filter = Filters.and(Arrays.asList(Filters.and(timestampClauses), OQLTimeSeriesFilterBuilder.getFilter(request.getOqlFilter(), attributesPrefixRemoval, ignoreFilterAttributes)));
            SearchOrder searchOrder = new SearchOrder("begin", 1);
            // Iterate over each measurement and ingest it again
            measurementCollection.find(filter, searchOrder, null, null, 0).forEach(measurement -> {
                count.increment();
                timeSeriesBucketingHandler.ingestExistingMeasurement(measurement);
            });
            ingestionPipeline.flush();
        }

        TimeSeriesAggregationPipeline aggregationPipeline = new TimeSeriesAggregationPipeline(inmemoryBuckets, 5000);
        TimeSeriesAggregationQuery query = mapToQuery(request, aggregationPipeline);
        TimeSeriesAggregationResponse response = query.run();

        return mapToApiResponse(request, response);
    }

    private TimeSeriesAPIResponse mapToApiResponse(FetchBucketsRequest request, TimeSeriesAggregationResponse response) {
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

    @Secured(right = "execution-read")
    @GET
    @Path("/oql-verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public OQLVerifyResponse verifyOql(@QueryParam("oql") String oql) {
        List<String> timeSeriesAttributes = this.timeSeriesAttributes
                .stream()
                .map(x -> "attributes." + x)
                .collect(Collectors.toList());
        boolean isValid = true;
        boolean hasUnknownFields = false;
        Set<String> fields = Collections.emptySet();
        if (StringUtils.isNotEmpty(oql)) {
            try {
                fields = new HashSet<>(OQLTimeSeriesFilterBuilder.getFilterAttributes(oql));
                hasUnknownFields = !timeSeriesAttributes.containsAll(fields);

            } catch (IllegalStateException e) {
                isValid = false;
            }
        }
        return new OQLVerifyResponse(isValid, hasUnknownFields, fields);
    }

    private Set<String> getFilterAttributes(Filter filter) {
        return Set.of();
    }

    private void validateFetchRequest(FetchBucketsRequest request) {
        if (request.getStart() == null || request.getEnd() == null) {
            throw new ControllerServiceException("Start and End parameters must be specified");
        }
        if (request.getStart() > request.getEnd()) {
            throw new ControllerServiceException("Start value must be lower than End");
        }
    }

    private TimeSeriesAggregationQuery mapToQuery(FetchBucketsRequest request,
                                                  TimeSeriesAggregationPipeline pipeline) {
        TimeSeriesAggregationQueryBuilder timeSeriesAggregationQuery = pipeline.newQueryBuilder()
                .range(request.getStart(), request.getEnd())
                .withFilter(Filters.and(
                        Arrays.asList(
                                TimeSeriesFilterBuilder.buildFilter(request.getOqlFilter()),
                                TimeSeriesFilterBuilder.buildFilter(request.getParams()))
                        ))
                .withGroupDimensions(request.getGroupDimensions());
        if (request.getIntervalSize() > 0) {
            timeSeriesAggregationQuery.window(request.getIntervalSize());
        }
        if (request.getNumberOfBuckets() != null) {
            timeSeriesAggregationQuery.split(request.getNumberOfBuckets());
        }
        return timeSeriesAggregationQuery.build();
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
            //Update execution
            Execution execution = executionAccessor.get(executionId);
            execution.addCustomField(TIMESERIES_FLAG, true);
            executionAccessor.save(execution);
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
                        List<String> attributes = Arrays.asList(configuration.getProperty(TIME_SERIES_ATTRIBUTES_PROPERTY, TIME_SERIES_ATTRIBUTES_DEFAULT).split(","));
                        TimeSeriesBucketingHandler timeSeriesBucketingHandler = new TimeSeriesBucketingHandler(ingestionPipeline, attributes);
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
        Execution execution = executionAccessor.get(executionId);
        if (execution == null) {
            throw new ControllerServiceException("No execution found matching this execution id");
        }
        Boolean hasTimeSeries = execution.getCustomField(TIMESERIES_FLAG, Boolean.class);
        return hasTimeSeries!=null && hasTimeSeries;
    }
}
