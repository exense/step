package step.plugins.timeseries.api;

import jakarta.validation.constraints.NotNull;
import step.core.timeseries.bucket.BucketAttributes;

import java.util.List;
import java.util.Set;

public class TimeSeriesAPIResponse {

    @NotNull
    private final long start;
    @NotNull
    private final long interval;
    @NotNull
    private final long end;
    @NotNull
    private final List<List<BucketResponse>> matrix;
    @NotNull
    private final List<BucketAttributes> matrixKeys;
    @NotNull
    private final boolean truncated;
    @NotNull
    private final long collectionResolution;
    @NotNull
    private final Set<String> collectionIgnoredAttributes;
    @NotNull
    private final boolean higherResolutionUsed;
    @NotNull
    private final boolean ttlCovered;

    public TimeSeriesAPIResponse(long start, long interval, long end, List<List<BucketResponse>> matrix, List<BucketAttributes> matrixKeys, boolean truncated, long collectionResolution, Set<String> collectionIgnoredAttributes, boolean higherResolutionUsed, boolean ttlCovered) {
        this.start = start;
        this.interval = interval;
        this.end = end;
        this.matrix = matrix;
        this.matrixKeys = matrixKeys;
        this.truncated = truncated;
        this.collectionResolution = collectionResolution;
        this.collectionIgnoredAttributes = collectionIgnoredAttributes;
        this.higherResolutionUsed = higherResolutionUsed;
        this.ttlCovered = ttlCovered;
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

    public List<List<BucketResponse>> getMatrix() {
        return matrix;
    }

    public List<BucketAttributes> getMatrixKeys() {
        return matrixKeys;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public boolean isHigherResolutionUsed() {
        return higherResolutionUsed;
    }

    public long getCollectionResolution() {
        return collectionResolution;
    }

    public @NotNull Set<String> getCollectionIgnoredAttributes() {
        return collectionIgnoredAttributes;
    }

    @NotNull
    public boolean isTtlCovered() {
        return ttlCovered;
    }
}
