package step.plugins.timeseries.api;

import java.util.*;

public class FetchBucketsRequest {
    private Long start;
    private Long end;
    private String oqlFilter;
    private Map<String, Object> params;
    private Set<String> groupDimensions = new HashSet<>();
    private Integer numberOfBuckets;
    private long intervalSize; // in ms
    private List<Integer> percentiles = Collections.emptyList();
    private Set<String> collectAttributeKeys;
    private int collectAttributesValuesLimit;
    private int maxNumberOfSeries;

    public Long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public Set<String> getGroupDimensions() {
        return groupDimensions;
    }

    public void setGroupDimensions(Set<String> groupDimensions) {
        this.groupDimensions = groupDimensions;
    }

    public Integer getNumberOfBuckets() {
        return numberOfBuckets;
    }

    public void setNumberOfBuckets(Integer numberOfBuckets) {
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

    public String getOqlFilter() {
        return oqlFilter;
    }

    public FetchBucketsRequest setOqlFilter(String oqlFilter) {
        this.oqlFilter = oqlFilter;
        return this;
    }

    public Set<String> getCollectAttributeKeys() {
        return collectAttributeKeys;
    }

    public void setCollectAttributeKeys(Set<String> collectAttributeKeys) {
        this.collectAttributeKeys = collectAttributeKeys;
    }

    public int getCollectAttributesValuesLimit() {
        return collectAttributesValuesLimit;
    }

    public void setCollectAttributesValuesLimit(int collectAttributesValuesLimit) {
        this.collectAttributesValuesLimit = collectAttributesValuesLimit;
    }

    public int getMaxNumberOfSeries() {
        return maxNumberOfSeries;
    }

    public void setMaxNumberOfSeries(int maxNumberOfSeries) {
        this.maxNumberOfSeries = maxNumberOfSeries;
    }
}
