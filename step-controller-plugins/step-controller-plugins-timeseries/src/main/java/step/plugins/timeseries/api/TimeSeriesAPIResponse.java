package step.plugins.timeseries.api;

import step.core.timeseries.Bucket;
import step.core.timeseries.BucketAttributes;

import java.util.List;

public class TimeSeriesAPIResponse {

    private final long start;
    private final long interval;
    private final long end;
    private final List<BucketResponse[]> matrix;
    private final List<BucketAttributes> matrixKeys;

    public TimeSeriesAPIResponse(long start, long interval, long end, List<BucketResponse[]> matrix, List<BucketAttributes> matrixKeys) {
        this.start = start;
        this.interval = interval;
        this.end = end;
        this.matrix = matrix;
        this.matrixKeys = matrixKeys;
    }

    public long getStart() {
        return start;
    }

    public long getInterval() {
        return interval;
    }

    public long getEnd() {
        return end;
    }

    public List<BucketResponse[]> getMatrix() {
        return matrix;
    }

    public List<BucketAttributes> getMatrixKeys() {
        return matrixKeys;
    }
}
