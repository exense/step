package step.plugins.timeseries.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import step.core.GlobalContext;
import step.core.deployment.AbstractServices;
import step.core.timeseries.Bucket;
import step.core.timeseries.Query;
import step.core.timeseries.TimeSeriesChartResponse;
import step.core.timeseries.accessor.BucketAccessor;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.Map;

@Singleton
@Path("/time-series")
@Tag(name = "TimeSeries")
public class TimeSeriesService extends AbstractServices {

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
    public TimeSeriesChartResponse getBucketsNew(FetchBucketsRequest request) {
        Query query = mapToQuery(request);
        return bucketAccessor.collect(query);
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
