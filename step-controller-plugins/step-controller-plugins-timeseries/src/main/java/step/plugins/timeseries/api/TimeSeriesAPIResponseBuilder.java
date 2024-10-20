package step.plugins.timeseries.api;


import step.core.timeseries.bucket.BucketAttributes;

import java.util.List;
import java.util.Objects;

public final class TimeSeriesAPIResponseBuilder {
    private Long start;
    private Long interval;
    private Long end;
    private List<List<BucketResponse>> matrix;
    private List<BucketAttributes> matrixKeys;
    private boolean truncated;
    private long collectionResolution;
    private boolean higherResolutionUsed;
    private boolean ttlCovered = true;

    public TimeSeriesAPIResponseBuilder withStart(long start) {
        this.start = start;
        return this;
    }

    public TimeSeriesAPIResponseBuilder withInterval(long interval) {
        this.interval = interval;
        return this;
    }

    public TimeSeriesAPIResponseBuilder withEnd(long end) {
        this.end = end;
        return this;
    }

    public TimeSeriesAPIResponseBuilder withMatrix(List<List<BucketResponse>> matrix) {
        this.matrix = matrix;
        return this;
    }

    public TimeSeriesAPIResponseBuilder withMatrixKeys(List<BucketAttributes> matrixKeys) {
        this.matrixKeys = matrixKeys;
        return this;
    }

    public TimeSeriesAPIResponseBuilder withTruncated(boolean truncated) {
        this.truncated = truncated;
        return this;
    }

    public TimeSeriesAPIResponseBuilder withHigherResolutionUsed(boolean higherResolutionUsed) {
        this.higherResolutionUsed = higherResolutionUsed;
        return this;
    }

    public TimeSeriesAPIResponseBuilder withCollectionResolution(long collectionResolution) {
        this.collectionResolution = collectionResolution;
        return this;
    }

    public TimeSeriesAPIResponseBuilder withTtlCovered(boolean ttlCovered) {
        this.ttlCovered = ttlCovered;
        return this;
    }

    public TimeSeriesAPIResponse build() {
        Objects.requireNonNull(start);
        Objects.requireNonNull(interval);
        Objects.requireNonNull(end);
        Objects.requireNonNull(matrix);
        Objects.requireNonNull(matrixKeys);
        return new TimeSeriesAPIResponse(start, interval, end, matrix, matrixKeys, truncated, collectionResolution, higherResolutionUsed, ttlCovered);
    }
}
