package step.plugins.timeseries.api;

import step.core.timeseries.BucketAttributes;

import java.util.Map;
import java.util.Objects;

public final class BucketResponseBuilder {
    private Long begin;
    private BucketAttributes attributes;
    private long count;
    private long sum;
    private long min;
    private long max;
    private Map<Integer, Long> pclValues;
    private long throughputPerHour;


    public BucketResponseBuilder withBegin(long begin) {
        this.begin = begin;
        return this;
    }

    public BucketResponseBuilder withAttributes(BucketAttributes attributes) {
        this.attributes = attributes;
        return this;
    }

    public BucketResponseBuilder withCount(long count) {
        this.count = count;
        return this;
    }

    public BucketResponseBuilder withSum(long sum) {
        this.sum = sum;
        return this;
    }

    public BucketResponseBuilder withMin(long min) {
        this.min = min;
        return this;
    }

    public BucketResponseBuilder withMax(long max) {
        this.max = max;
        return this;
    }

    public BucketResponseBuilder withPclValues(Map<Integer, Long> withPclValues) {
        this.pclValues = withPclValues;
        return this;
    }

    public BucketResponseBuilder withThroughputPerHour(long throughputPerHour) {
        this.throughputPerHour = throughputPerHour;
        return this;
    }

    public BucketResponse build() {
        Objects.requireNonNull(begin);
        return new BucketResponse(begin, attributes, count, sum, min, max, pclValues, throughputPerHour);
    }
}
