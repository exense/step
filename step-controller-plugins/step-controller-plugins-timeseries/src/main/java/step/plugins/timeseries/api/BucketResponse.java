package step.plugins.timeseries.api;


import jakarta.validation.constraints.NotNull;
import step.core.timeseries.bucket.BucketAttributes;

import java.util.HashMap;
import java.util.Map;

public class BucketResponse {

    @NotNull
    private final long begin;
    @NotNull
    private final BucketAttributes attributes;
    @NotNull
    private final long count;
    @NotNull
    private final long sum;
    @NotNull
    private final long min;
    @NotNull
    private final long max;

    private final Map<Integer, Long> pclValues;
    @NotNull
    private final long throughputPerHour;

    public BucketResponse(long begin, BucketAttributes attributes, long count, long sum, long min, long max, Map<Integer, Long> getPclValues, long throughputPerHour) {
        this.begin = begin;
        this.attributes = attributes != null ? attributes : new BucketAttributes();
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
        this.pclValues = getPclValues;
        this.throughputPerHour = throughputPerHour;
    }

    public long getBegin() {
        return begin;
    }

    public BucketAttributes getAttributes() {
        return attributes;
    }

    public long getCount() {
        return count;
    }

    public long getSum() {
        return sum;
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    public Map<Integer, Long> getPclValues() {
        return pclValues;
    }

    public long getThroughputPerHour() {
        return throughputPerHour;
    }
}
