package step.plugins.timeseries.api;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FetchBucketsRequest {
    private long start;
    private long end;
    private Map<String, String> params;
    private Set<String> groupDimensions = new HashSet<>();
    private Long numberOfBuckets;
    private long intervalSize; // in ms

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public Set<String> getGroupDimensions() {
        return groupDimensions;
    }

    public void setGroupDimensions(Set<String> groupDimensions) {
        this.groupDimensions = groupDimensions;
    }

    public Long getNumberOfBuckets() {
        return numberOfBuckets;
    }

    public void setNumberOfBuckets(Long numberOfBuckets) {
        this.numberOfBuckets = numberOfBuckets;
    }

    public long getIntervalSize() {
        return intervalSize;
    }

    public void setIntervalSize(long intervalSize) {
        this.intervalSize = intervalSize;
    }
}
