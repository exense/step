package step.plugins.timeseries.api;

import step.core.timeseries.BucketAttributes;

import java.util.List;
import java.util.Objects;

public final class TimeSeriesAPIResponseBuilder {
    private Long start;
    private Long interval;
    private Long end;
    private List<List<BucketResponse>> matrix;
    private List<BucketAttributes> matrixKeys;

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

    public TimeSeriesAPIResponse build() {
        Objects.requireNonNull(start);
        Objects.requireNonNull(interval);
        Objects.requireNonNull(end);
        Objects.requireNonNull(matrix);
        Objects.requireNonNull(matrixKeys);
        return new TimeSeriesAPIResponse(start, interval, end, matrix, matrixKeys);
    }
}
