package step.plugins.timeseries.api;

import java.util.*;

public class FetchBucketsRequest {
    private long start;
    private long end;
    private Map<String, String> params;
    private boolean threadGroupBuckets = false;
    private Set<String> groupDimensions = new HashSet<>();
    private Long numberOfBuckets;
    private long intervalSize; // in ms
    private List<Integer> percentiles = Collections.emptyList();

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

    public boolean isThreadGroupBuckets() {
        return threadGroupBuckets;
    }

    public void setThreadGroupBuckets(boolean threadGroupBuckets) {
        this.threadGroupBuckets = threadGroupBuckets;
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

    public List<Integer> getPercentiles() {
        return percentiles;
    }

    public FetchBucketsRequest setPercentiles(List<Integer> percentiles) {
        this.percentiles = percentiles;
        return this;
    }
}
