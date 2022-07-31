package step.plugins.timeseries.api;

import step.core.timeseries.BucketAttributes;

import java.util.Map;

public class BucketResponse {

    private final long begin;
    private final BucketAttributes attributes;
    private final long count;
    private final long sum;
    private final long min;
    private final long max;
    // TODO rename to pclValues
    private final Map<Integer, Long> pclValues;

    public BucketResponse(long begin, BucketAttributes attributes, long count, long sum, long min, long max, Map<Integer, Long> getPclValues) {
        this.begin = begin;
        this.attributes = attributes;
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
        this.pclValues = getPclValues;
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
}
