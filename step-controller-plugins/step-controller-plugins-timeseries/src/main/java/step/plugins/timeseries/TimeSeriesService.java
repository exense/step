package step.plugins.timeseries;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
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

import static step.plugins.timeseries.TimeSeriesControllerPlugin.*;

@Singleton
@Path("/time-series")
@Tag(name = "TimeSeries")
public class TimeSeriesService extends AbstractStepServices {


    private TimeSeriesHandler handler;
    private MetricTypeAccessor metricTypeAccessor;
    private int maxNumberOfSeries;

    public static final String TIME_SERIES_SAMPLING_LIMIT = "timeseries.sampling.limit";
    public static final String TIME_SERIES_MAX_NUMBER_OF_SERIES = "timeseries.response.series.limit";

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        List<String> timeSeriesAttributes = context.get(TimeSeriesBucketingHandler.class).getHandledAttributes();
        AsyncTaskManager asyncTaskManager = context.require(AsyncTaskManager.class);
        Collection<Measurement> measurementCollection = context.getCollectionFactory().getCollection(EntityManager.measurements, Measurement.class);
        metricTypeAccessor = context.require(MetricTypeAccessor.class);
        TimeSeries timeSeries = context.require(TimeSeries.class);
        ExecutionAccessor executionAccessor = context.getExecutionAccessor();
        int resolution = (int) timeSeries.getIngestionPipeline().getResolution();
        int fieldsSamplingLimit = configuration.getPropertyAsInteger(TIME_SERIES_SAMPLING_LIMIT, 1000);
        maxNumberOfSeries = configuration.getPropertyAsInteger(TIME_SERIES_MAX_NUMBER_OF_SERIES, 1000);
        this.handler = new TimeSeriesHandler(resolution, timeSeriesAttributes, measurementCollection, executionAccessor, timeSeries, asyncTaskManager, fieldsSamplingLimit);
    }

    @Secured(right = "execution-read")
    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TimeSeriesAPIResponse getTimeSeries(@NotNull FetchBucketsRequest request) {
        enrichRequest(request);
        return handler.getTimeSeries(request);
    }
    
    @Secured(right = "execution-read")
    @POST
    @Path("/measurements")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // TODO this method should be renamed as it doesn't return measurements but a timeseries
    public TimeSeriesAPIResponse getMeasurements(@NotNull FetchBucketsRequest request) {
        enrichRequest(request);
        return handler.getOrBuildTimeSeries(request);
    }

    private void enrichRequest(FetchBucketsRequest request) {
        request.setOqlFilter(enrichOqlFilter(request.getOqlFilter()));
        if (request.getMaxNumberOfSeries() <= 0) {
            request.setMaxNumberOfSeries(maxNumberOfSeries);
        }
    }

    private String enrichOqlFilter(String oqlFilter) {
        String additionalOqlFilter = getObjectFilter().getOQLFilter();
        if (StringUtils.isNotEmpty(additionalOqlFilter)) {
            return (StringUtils.isNotEmpty(oqlFilter)) ?
                oqlFilter + " and (" + additionalOqlFilter + ")" :
                "(" + additionalOqlFilter + ")";
        }
        return oqlFilter;
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
    public AsyncTaskStatus<Object> rebuildTimeSeries(@NotNull TimeSeriesRebuildRequest request) {
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
        oqlFilter = enrichOqlFilter(oqlFilter);
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
        oqlFilter = enrichOqlFilter(oqlFilter);
        return handler.getRawMeasurements(oqlFilter, skip, limit);
    }

    @Secured(right = "execution-read")
    @GET
    @Path("/raw-measurements/stats")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public MeasurementsStats getRawMeasurementsStats(@QueryParam("filter") String oqlFilter) {
        oqlFilter = enrichOqlFilter(oqlFilter);
        return handler.getRawMeasurementsStats(oqlFilter);
    }

    @Operation(description = "Returns the list of all supported metric types")
    @Secured()
    @GET
    @Path("/metric-types")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<MetricType> getMetricTypes() {
        return metricTypeAccessor.stream().collect(Collectors.toList());
    }


}
