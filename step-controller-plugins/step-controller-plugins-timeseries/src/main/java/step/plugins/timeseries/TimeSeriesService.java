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
import step.core.timeseries.Bucket;
import step.core.timeseries.Query;
import step.core.timeseries.TimeSeriesChartResponse;
import step.core.timeseries.accessor.BucketAccessor;
import step.plugins.timeseries.api.*;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@Path("/time-series")
@Tag(name = "TimeSeries")
public class TimeSeriesService extends AbstractStepServices {

    protected BucketAccessor bucketAccessor;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        bucketAccessor = context.get(BucketAccessor.class);
    }

    @POST
    @Path("/buckets")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Map<String, Object>, Map<Long, Bucket>> getBuckets(FetchBucketsRequest request) {
        Query query = mapToQuery(request);
        return bucketAccessor.collectBuckets(query);
    }

    @POST
    @Path("/buckets-new")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TimeSeriesAPIResponse getBucketsNew(FetchBucketsRequest request) {
        Query query = mapToQuery(request);
        TimeSeriesChartResponse chartResponse = bucketAccessor.collect(query);
        return new TimeSeriesAPIResponseBuilder()
                .withStart(chartResponse.getStart())
                .withEnd(chartResponse.getEnd())
                .withInterval(chartResponse.getInterval())
                .withMatrixKeys(chartResponse.getMatrixKeys())
                .withMatrix(chartResponse.getMatrix()
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

    private Query mapToQuery(FetchBucketsRequest request) {
        return new Query()
                .range(request.getStart(), request.getEnd())
                .withThreadGroupsBuckets(request.isThreadGroupBuckets())
                .window(request.getIntervalSize())
                .filter(request.getParams() != null ? request.getParams() : Collections.emptyMap())
                .split(request.getNumberOfBuckets())
                .groupBy(request.getGroupDimensions());
    }
}
