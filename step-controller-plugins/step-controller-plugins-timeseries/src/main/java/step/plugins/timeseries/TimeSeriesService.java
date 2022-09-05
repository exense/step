package step.plugins.timeseries;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import step.core.GlobalContext;
import step.core.deployment.AbstractStepServices;
import step.core.timeseries.TimeSeriesAggregationPipeline;
import step.core.timeseries.TimeSeriesAggregationQuery;
import step.core.timeseries.TimeSeriesAggregationResponse;
import step.framework.server.security.Secured;
import step.plugins.timeseries.api.*;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@Path("/time-series")
@Tag(name = "TimeSeries")
public class TimeSeriesService extends AbstractStepServices {

    private TimeSeriesAggregationPipeline aggregationPipeline;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        aggregationPipeline = context.get(TimeSeriesAggregationPipeline.class);
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
}
