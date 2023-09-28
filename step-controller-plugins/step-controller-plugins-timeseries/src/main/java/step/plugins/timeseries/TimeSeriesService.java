package step.plugins.timeseries;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import step.controller.services.async.AsyncTaskManager;
import step.controller.services.async.AsyncTaskStatus;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.deployment.AbstractStepServices;
import step.core.entities.EntityManager;
import step.core.execution.model.ExecutionAccessor;
import step.core.timeseries.*;
import step.core.timeseries.aggregation.TimeSeriesAggregationPipeline;
import step.core.timeseries.metric.MetricType;
import step.core.timeseries.metric.MetricTypeAccessor;
import step.framework.server.security.Secured;
import step.plugins.measurements.Measurement;
import step.plugins.timeseries.api.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static step.plugins.timeseries.TimeSeriesControllerPlugin.RESOLUTION_PERIOD_PROPERTY;
import static step.plugins.timeseries.TimeSeriesControllerPlugin.TIME_SERIES_SAMPLING_LIMIT;

@Singleton
@Path("/time-series")
@Tag(name = "TimeSeries")
public class TimeSeriesService extends AbstractStepServices {


    private TimeSeriesHandler handler;
    private MetricTypeAccessor metricTypeAccessor;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        List<String> timeSeriesAttributes = context.get(TimeSeriesBucketingHandler.class).getHandledAttributes();
        TimeSeriesAggregationPipeline aggregationPipeline = context.require(TimeSeriesAggregationPipeline.class);
        AsyncTaskManager asyncTaskManager = context.require(AsyncTaskManager.class);
        Collection<Measurement> measurementCollection = context.getCollectionFactory().getCollection(EntityManager.measurements, Measurement.class);
        metricTypeAccessor = context.get(MetricTypeAccessor.class);
        TimeSeries timeSeries = context.require(TimeSeries.class);
        ExecutionAccessor executionAccessor = context.getExecutionAccessor();
        int resolution = configuration.getPropertyAsInteger(RESOLUTION_PERIOD_PROPERTY, 1000);
        int fieldsSamplingLimit = configuration.getPropertyAsInteger(TIME_SERIES_SAMPLING_LIMIT, 1000);
        this.handler = new TimeSeriesHandler(resolution, timeSeriesAttributes, measurementCollection, executionAccessor, timeSeries, aggregationPipeline, asyncTaskManager, fieldsSamplingLimit);
    }

    @Secured(right = "execution-read")
    @POST
    @Path("/buckets")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TimeSeriesAPIResponse getBuckets(FetchBucketsRequest request) {
        return handler.getBuckets(request);
    }

    /**
     * Return details about the provided OQL, like if it's valid or not, found attributes, etc
     */
    @Secured(right = "execution-read")
    @GET
    @Path("/oql-verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public OQLVerifyResponse verifyOql(@NotNull @QueryParam("oql") String oql) {
        return handler.verifyOql(oql);
    }

    @Operation(operationId = "rebuildTimeSeries", description = "Rebuild a time series based on the provided request")
    @Secured(right = "execution-read")
    @POST
    @Path("/rebuild")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AsyncTaskStatus<Object> rebuildTimeSeries(TimeSeriesRebuildRequest request) {
        String executionId = Objects.requireNonNull(request.getExecutionId(), "executionId not specified");
        return handler.rebuildTimeSeries(executionId);
    }

    @Operation(operationId = "checkTimeSeries", description = "Check if the time-series was created for a specific execution")
    @Secured(right = "execution-read")
    @GET
    @Path("/execution/{executionId}/exists")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public boolean timeSeriesIsBuilt(@PathParam("executionId") String executionId) {
        return handler.timeSeriesIsBuilt(executionId);
    }

    @Secured(right = "execution-read")
    @GET
    @Path("/measurements-fields")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getMeasurementsAttributes(@QueryParam("filter") String oqlFilter) {
        return handler.getMeasurementsAttributes(oqlFilter);
    }

    @Secured(right = "execution-read")
    @GET
    @Path("/raw-measurements")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Measurement> discoverMeasurements(
            @QueryParam("filter") String oqlFilter,
            @QueryParam("limit") int limit,
            @QueryParam("skip") int skip
    ) {
        return handler.getRawMeasurements(oqlFilter, skip, limit);
    }

    @Secured(right = "execution-read")
    @GET
    @Path("/raw-measurements/stats")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public MeasurementsStats getRawMeasurementsStats(@QueryParam("filter") String oqlFilter) {
        return handler.getRawMeasurementsStats(oqlFilter);
    }
    
    @Secured(right = "execution-read")
    @GET
    @Path("/metric-types")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<MetricType> getMetricTypes() {
        return metricTypeAccessor.stream().collect(Collectors.toList());
    }


}
