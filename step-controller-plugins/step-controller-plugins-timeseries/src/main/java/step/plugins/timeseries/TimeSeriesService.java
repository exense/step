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
import step.core.execution.model.ExecutionAccessor;
import step.core.timeseries.*;
import step.core.timeseries.aggregation.TimeSeriesAggregationPipeline;
import step.framework.server.security.Secured;
import step.plugins.measurements.Measurement;
import step.plugins.timeseries.api.*;

import java.util.*;

import static step.plugins.timeseries.TimeSeriesControllerPlugin.RESOLUTION_PERIOD_PROPERTY;

@Singleton
@Path("/time-series")
@Tag(name = "TimeSeries")
public class TimeSeriesService extends AbstractStepServices {


    private TimeSeriesHandler handler;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        List<String> timeSeriesAttributes = context.get(TimeSeriesBucketingHandler.class).getHandledAttributes();
        TimeSeriesAggregationPipeline aggregationPipeline = context.require(TimeSeriesAggregationPipeline.class);
        AsyncTaskManager asyncTaskManager = context.require(AsyncTaskManager.class);
        Collection<Measurement> measurementCollection = context.getCollectionFactory().getCollection(MeasurementAccessor.ENTITY_NAME, Measurement.class);
        TimeSeries timeSeries = context.require(TimeSeries.class);
        ExecutionAccessor executionAccessor = context.getExecutionAccessor();
        int resolution = configuration.getPropertyAsInteger(RESOLUTION_PERIOD_PROPERTY, 1000);
        this.handler = new TimeSeriesHandler(resolution, timeSeriesAttributes, measurementCollection, executionAccessor, timeSeries, aggregationPipeline, asyncTaskManager);
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

}
