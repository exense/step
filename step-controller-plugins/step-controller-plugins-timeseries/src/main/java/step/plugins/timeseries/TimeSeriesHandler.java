package step.plugins.timeseries;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import step.controller.services.async.AsyncTaskManager;
import step.controller.services.async.AsyncTaskStatus;
import step.core.artefacts.reports.aggregated.ReportNodeTimeSeries;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.collections.SearchOrder;
import step.core.collections.filters.Equals;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.deployment.ControllerServiceException;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.TimeSeriesBuilder;
import step.core.timeseries.TimeSeriesCollection;
import step.core.timeseries.TimeSeriesFilterBuilder;
import step.core.timeseries.aggregation.TimeSeriesAggregationPipeline;
import step.core.timeseries.aggregation.TimeSeriesAggregationQuery;
import step.core.timeseries.aggregation.TimeSeriesAggregationQueryBuilder;
import step.core.timeseries.aggregation.TimeSeriesAggregationResponse;
import step.core.timeseries.bucket.Bucket;
import step.core.timeseries.bucket.BucketAttributes;
import step.core.timeseries.query.OQLTimeSeriesFilterBuilder;
import step.plugins.measurements.ExecutionMetricSample;
import step.plugins.measurements.Measurement;
import step.plugins.measurements.SamplesExecutionPlugin;
import step.plugins.timeseries.api.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static step.core.timeseries.TimeSeriesConstants.ATTRIBUTES_PREFIX;
import static step.core.timeseries.TimeSeriesConstants.TIMESTAMP_ATTRIBUTE;
import static step.plugins.measurements.AbstractMetricSample.METRIC_TYPE;
import static step.plugins.measurements.SamplesExecutionPlugin.ATTRIBUTE_EXECUTION_ID;
import static step.plugins.timeseries.TimeSeriesExecutionPlugin.TIMESERIES_FLAG;

public class TimeSeriesHandler {

    private static final List<String> MEASUREMENTS_FILTER_IGNORE_ATTRIBUTES = List.of(METRIC_TYPE);
    private static final Function<String, String> attributesPrefixRemoval = (attribute) -> {
        if (attribute.startsWith(ATTRIBUTES_PREFIX)) {
            return attribute.replaceFirst(ATTRIBUTES_PREFIX, "");
        } else {
            return attribute;
        }
    };
    //Ugly implementation
    private static final List<String> executionMetricSampleBaseFields = List.of("eId","rnId","planId","plan","taskId","schedule","execution","agentUrl","origin", "metricType");
    private static final List<String> executionMetricSampleAttributesFields = List.of("project","projectName");
    private static final Function<String, String> attributesPrefixRemovalSamples = (attribute) -> {
        if (attribute.startsWith(ATTRIBUTES_PREFIX)) {
            String attributeRenamed = attribute.replaceFirst(ATTRIBUTES_PREFIX, "");
            if (executionMetricSampleBaseFields.contains(attributeRenamed)) {
                return attributeRenamed;
            } else if (executionMetricSampleAttributesFields.contains(attributeRenamed)) {
                return attribute;
            } else if ("name".equals(attributeRenamed)){
                return "sample.name";
            } else {
                return "sample.labels." + attributeRenamed;
            }
        } else {
            return attribute;
        }
    };
    public static final String SAMPLE_SAMPLE_TIME = "sample.sampleTime";
    private final Set<String> includedAttributesWithPrefix;
    private final Set<String> exclduedAttributesWithPrefix;

    private final Set<String> timeSeriesIncludedAttributes;
    private final Set<String> timeSeriesExcludedAttributes;
    private final AsyncTaskManager asyncTaskManager;
    private final TimeSeriesAggregationPipeline aggregationPipeline;
    private final TimeSeriesAggregationPipeline reportNodeAggregationPipeline;
    private final step.core.collections.Collection<Measurement> measurementCollection;
    private final step.core.collections.Collection<ExecutionMetricSample> metricSampleCollection;
    private final ExecutionAccessor executionAccessor;
    private final TimeSeries timeSeries;
    private final int resolution;
    private final int samplingLimit;

    public TimeSeriesHandler(int resolution,
                             Set<String> timeSeriesIncludedAttributes,
                             Set<String> timeSeriesExcludedAttributes,
                             step.core.collections.Collection<Measurement> measurementCollection,
                             step.core.collections.Collection<ExecutionMetricSample> metricSampleCollection,
                             ExecutionAccessor executionAccessor,
                             TimeSeries timeSeries,
                             ReportNodeTimeSeries reportNodeTimeSeries,
                             AsyncTaskManager asyncTaskManager,
                             int samplingLimit) {
        this.resolution = resolution;
        this.timeSeriesIncludedAttributes = timeSeriesIncludedAttributes;
        this.timeSeriesExcludedAttributes = timeSeriesExcludedAttributes;
        this.measurementCollection = measurementCollection;
        this.metricSampleCollection = metricSampleCollection;
        this.aggregationPipeline = timeSeries.getAggregationPipeline();
        this.reportNodeAggregationPipeline = reportNodeTimeSeries.getTimeSeries().getAggregationPipeline();
        this.executionAccessor = executionAccessor;
        this.asyncTaskManager = asyncTaskManager;
        this.timeSeries = timeSeries;
        this.samplingLimit = samplingLimit;
        this.includedAttributesWithPrefix = this.timeSeriesIncludedAttributes
            .stream()
            .map(x -> ATTRIBUTES_PREFIX + x)
            .collect(Collectors.toSet());
        this.exclduedAttributesWithPrefix = this.timeSeriesExcludedAttributes
            .stream()
            .map(x -> ATTRIBUTES_PREFIX + x)
            .collect(Collectors.toSet());
    }

    /**
     * This method fetches and group the RAW measurements into a bucket structure.
     */
    private TimeSeriesAPIResponse getTimeSeriesFromRawMeasurements(FetchBucketsRequest request, Collection<String> fields) {
        int resolutionMs = getResolution(request);
        step.core.collections.Collection<Bucket> inmemoryBuckets = new InMemoryCollection<>();
        TimeSeriesCollection tsCollection = new TimeSeriesCollection(inmemoryBuckets, resolutionMs);
        try (TimeSeries timeSeries = new TimeSeriesBuilder()
            .registerCollection(tsCollection)
            .build()) {

            Set<String> standardAttributes = new HashSet<>(timeSeriesIncludedAttributes);
            standardAttributes.addAll(fields.stream().map(attributesPrefixRemoval).collect(Collectors.toList()));
            standardAttributes.addAll(request.getGroupDimensions());
            TimeSeriesBucketingHandler timeSeriesBucketingHandler = new TimeSeriesBucketingHandler(timeSeries, standardAttributes, Set.of());
            LongAdder count = new LongAdder();
            Filter timestampClauses = Filters.empty();
            Filter samplesTimestampClauses = Filters.empty();
            Long start = request.getStart();
            Long end = request.getEnd();
            if (start != null && end != null) {
                timestampClauses = Filters.and(List.of(Filters.gte(TIMESTAMP_ATTRIBUTE, start), Filters.lt(TIMESTAMP_ATTRIBUTE, end)));
                samplesTimestampClauses = Filters.and(List.of(Filters.gte(SAMPLE_SAMPLE_TIME, start), Filters.lt(SAMPLE_SAMPLE_TIME, end)));
            } else if (start != null) {
                timestampClauses = Filters.gte(TIMESTAMP_ATTRIBUTE, start);
                samplesTimestampClauses = Filters.gte(SAMPLE_SAMPLE_TIME, start);
            } else if (end != null) {
                timestampClauses = Filters.lt(TIMESTAMP_ATTRIBUTE, end);
                samplesTimestampClauses = Filters.lt(SAMPLE_SAMPLE_TIME, end);
            }
            Filter oqlFilterMeasurements = OQLTimeSeriesFilterBuilder.getFilter(request.getOqlFilter(), attributesPrefixRemoval, MEASUREMENTS_FILTER_IGNORE_ATTRIBUTES);
            Filter oqlFilterMetricSamples = OQLTimeSeriesFilterBuilder.getFilter(request.getOqlFilter(), attributesPrefixRemovalSamples, MEASUREMENTS_FILTER_IGNORE_ATTRIBUTES);
            Filter filterMeasurements = Filters.and(List.of(timestampClauses, oqlFilterMeasurements));
            Filter filterMetricSamples = Filters.and(List.of(samplesTimestampClauses, oqlFilterMetricSamples));
            SearchOrder searchOrder = new SearchOrder(TIMESTAMP_ATTRIBUTE, 1);
            SearchOrder metricsSearchOrder = new SearchOrder(SAMPLE_SAMPLE_TIME, 1);
            // Iterate over each measurement and ingest it again
            try (Stream<Measurement> stream = measurementCollection.findLazy(filterMeasurements, searchOrder, null, null, 0)) {
                stream.forEach(measurement -> {
                    count.increment();
                    timeSeriesBucketingHandler.ingestExistingMeasurement(measurement);
                });
            }
            // Iterate over each metric samples and ingest it again if metricSampleCollection exists
            if (metricSampleCollection != null) {
                try (Stream<ExecutionMetricSample> stream = metricSampleCollection.findLazy(filterMetricSamples, metricsSearchOrder, null, null, 0)) {
                    stream.forEach(metricSample -> {
                        if (metricSample != null) {
                            count.increment();
                            timeSeriesBucketingHandler.processMetric(metricSample);
                        }
                    });
                }
            }
            timeSeriesBucketingHandler.flush();
            TimeSeriesAggregationPipeline aggregationPipeline = timeSeries.getAggregationPipeline();
            TimeSeriesAggregationQuery query = mapToQuery(request);
            TimeSeriesAggregationResponse response = aggregationPipeline.collect(query);

            return mapToApiResponse(request, response);
        }
    }

    public List<Measurement> getRawMeasurements(String oqlFilter, int skip, int limit) {
        Filter filter = OQLTimeSeriesFilterBuilder.getFilter(oqlFilter, attributesPrefixRemoval, MEASUREMENTS_FILTER_IGNORE_ATTRIBUTES);
        return measurementCollection.find(filter, null, skip, limit, 0)
            .collect(Collectors.toList());
    }

    public MeasurementsStats getRawMeasurementsStats(String oqlFilter) {
        Map<String, AttributeValuesStats> distinctAttributesStats = new HashMap<>();

        AtomicInteger totalCount = new AtomicInteger();

        Filter filter = OQLTimeSeriesFilterBuilder.getFilter(oqlFilter, attributesPrefixRemoval, MEASUREMENTS_FILTER_IGNORE_ATTRIBUTES);
        measurementCollection.find(filter, null, 0, samplingLimit, 0).forEach(m -> {
            m.forEach((key, value) -> {
                if (Objects.equals(key, SamplesExecutionPlugin.BEGIN)
                    || Objects.equals(key, SamplesExecutionPlugin.VALUE)
                    || Objects.equals(key, "_id")) {
                    return;
                }
                AttributeValuesStats fieldStats = distinctAttributesStats.computeIfAbsent(key, k -> new AttributeValuesStats());
                fieldStats.getValuesCount().computeIfAbsent(value, v -> new AtomicInteger()).incrementAndGet();
                fieldStats.incrementTotalCount();
            });
            totalCount.incrementAndGet();

        });
        Map<String, List<AttributeStats>> topValues = new HashMap<>();

        for (Map.Entry<String, AttributeValuesStats> entry : distinctAttributesStats.entrySet()) {
            String attribute = entry.getKey();
            AttributeValuesStats stats = entry.getValue();
            Map<Object, AtomicInteger> valueCounts = stats.getValuesCount();

            List<AttributeStats> percentages = valueCounts.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), 100.0 * e.getValue().get() / stats.getTotalCount()))
                .sorted(Map.Entry.<Object, Double>comparingByValue().reversed())
                .map(e -> new AttributeStats(e.getKey().toString(), e.getValue()))
                .limit(20)
                .collect(Collectors.toList());

            topValues.put(attribute, percentages);
        }
        return new MeasurementsStats(totalCount.get(), topValues.keySet(), topValues);
    }

    private int getResolution(FetchBucketsRequest request) {
        if (request.getIntervalSize() > 0) {
            return (int) request.getIntervalSize();
        }
        int nbBuckets = Math.max(100, request.getNumberOfBuckets());
        int calculatedResolution = (int) Math.floor((double) (request.getEnd() - request.getStart()) / nbBuckets);
        return Math.max(resolution, calculatedResolution);
    }

    public TimeSeriesAPIResponse getTimeSeries(FetchBucketsRequest request) {
        validateFetchRequest(request);
        TimeSeriesAggregationQuery query = mapToQuery(request);
        TimeSeriesAggregationResponse response = this.aggregationPipeline.collect(query);
        return mapToApiResponse(request, response);
    }

    public TimeSeriesAPIResponse getReportNodeTimeSeries(FetchBucketsRequest request) {
        validateFetchRequest(request);
        TimeSeriesAggregationQuery query = mapToQuery(request);
        TimeSeriesAggregationResponse response = this.reportNodeAggregationPipeline.collect(query);
        return mapToApiResponse(request, response);
    }

    /**
     * Gets the time series according to the provided request.
     * If the time series doesn't exist for the provided request, builds it based on the raw measurements
     *
     * @param request {@link FetchBucketsRequest}
     * @return {@link TimeSeriesAPIResponse}
     */
    public TimeSeriesAPIResponse getOrBuildTimeSeries(FetchBucketsRequest request) {
        validateFetchRequest(request);

        OQLVerifyResponse oqlVerifyResponse = this.verifyOql(request.getOqlFilter());
        if (!oqlVerifyResponse.isValid()) {
            throw new ControllerServiceException("Invalid OQL filter");
        } else if (oqlVerifyResponse.hasUnknownFields() || hasUnknownGroupDimensions(request.getGroupDimensions())) {
            // if the filter has attributes which are not indexed, switch to RAW measurements
            return this.getTimeSeriesFromRawMeasurements(request, oqlVerifyResponse.getFields());
        } else {
            return getTimeSeries(request);
        }
    }

    public boolean hasUnknownGroupDimensions(Set<String> groupDimensions) {
        if (timeSeriesIncludedAttributes.isEmpty()) {
            return groupDimensions.stream().anyMatch(timeSeriesExcludedAttributes::contains);
        } else {
            return !timeSeriesIncludedAttributes.containsAll(groupDimensions);
        }
    }

    public OQLVerifyResponse verifyOql(String oql) {

        boolean isValid = true;
        boolean hasUnknownFields = false;
        Set<String> oqlAttributes = Collections.emptySet();
        if (StringUtils.isNotEmpty(oql)) {
            try {
                oqlAttributes = new HashSet<>(OQLTimeSeriesFilterBuilder.getFilterAttributes(oql));
                //If the time-series are defined to only support a subset of a attributes we make sure all OQL fields are covered by the TS (or ultimately fallback to RAW measurements)
                if (!includedAttributesWithPrefix.isEmpty()) {
                    hasUnknownFields = !includedAttributesWithPrefix.containsAll(oqlAttributes);
                } else if (!exclduedAttributesWithPrefix.isEmpty()) {
                    hasUnknownFields =  exclduedAttributesWithPrefix.stream().anyMatch(oqlAttributes::contains);
                }
                if (oqlAttributes.isEmpty()) { // there are strings like 'abcd' which is a valid OQL by some reason
                    isValid = false;
                }
            } catch (IllegalStateException e) {
                isValid = false;
            }
        }
        return new OQLVerifyResponse(isValid, hasUnknownFields, oqlAttributes);
    }

    public Set<String> getMeasurementsAttributes(String oqlFilter) {
        Filter filter = OQLTimeSeriesFilterBuilder.getFilter(oqlFilter, attributesPrefixRemoval, Collections.emptySet());
        Set<String> fields = new HashSet<>();
        measurementCollection.find(filter, null, 0, samplingLimit, 0).forEach(measurement -> fields.addAll(measurement.keySet()));
        return fields;
    }

    public boolean timeSeriesIsBuilt(String executionId) {
        Execution execution = executionAccessor.get(executionId);
        if (execution == null) {
            throw new ControllerServiceException("No execution found matching this execution id");
        }
        Boolean hasTimeSeries = execution.getCustomField(TIMESERIES_FLAG, Boolean.class);
        return hasTimeSeries != null && hasTimeSeries;
    }

    public AsyncTaskStatus<Object> rebuildTimeSeries(String executionId) {
        if (this.timeSeriesIsBuilt(executionId)) {
            throw new ControllerServiceException("Time series already exist for this execution. Unable to rebuild it");
        } else {
            //Update execution
            Execution execution = executionAccessor.get(executionId);
            execution.addCustomField(TIMESERIES_FLAG, true);
            executionAccessor.save(execution);
            // we need to check if measurements exists
            Equals measurementFilter = Filters.equals(ATTRIBUTE_EXECUTION_ID, executionId);
            Measurement firstMeasurement = measurementCollection.find(measurementFilter,
                new SearchOrder(SamplesExecutionPlugin.BEGIN, 1), 0, 1, 0).findFirst().orElse(null);
            Measurement lastMeasurement = measurementCollection.find(measurementFilter,
                new SearchOrder(SamplesExecutionPlugin.BEGIN, -1), 0, 1, 0).findFirst().orElse(null);
            if (firstMeasurement != null && lastMeasurement != null) {
                return asyncTaskManager.scheduleAsyncTask(t -> {
                    // the flushing period can be a big value, because we will force flush every time.
                    TimeSeriesBucketingHandler timeSeriesBucketingHandler = new TimeSeriesBucketingHandler(timeSeries, timeSeriesIncludedAttributes, timeSeriesExcludedAttributes);
                    LongAdder count = new LongAdder();
                    SearchOrder searchOrder = new SearchOrder("begin", 1);
                    // Iterate over each measurement and ingest it again
                    try (Stream<Measurement> stream = measurementCollection.findLazy(measurementFilter, searchOrder, null, null, 0)) {
                        stream.forEach(measurement -> {
                            count.increment();
                            timeSeriesBucketingHandler.ingestExistingMeasurement(measurement);
                        });
                    }
                    timeSeries.getCollections().forEach(c -> {
                        // flush only the collections that handle execution id
                        if (CollectionUtils.isEmpty(c.getIgnoredAttributes()) || !c.getIgnoredAttributes().contains(ATTRIBUTE_EXECUTION_ID)) {
                            c.getIngestionPipeline().flush();
                        }
                    });
                    return new TimeSeriesRebuildResponse(count.longValue());
                });
            } else {
                throw new ControllerServiceException("No measurement found matching this execution id");
            }
        }
    }

    private TimeSeriesAggregationQuery mapToQuery(FetchBucketsRequest request) {
        TimeSeriesAggregationQueryBuilder timeSeriesAggregationQuery = new TimeSeriesAggregationQueryBuilder()
            .range(request.getStart(), request.getEnd())
            .withFilter(Filters.and(
                Arrays.asList(
                    TimeSeriesFilterBuilder.buildFilter(request.getOqlFilter()),
                    TimeSeriesFilterBuilder.buildFilter(request.getParams()))
            ))
            .withGroupDimensions(request.getGroupDimensions());
        if (request.getCollectAttributeKeys() != null && !request.getCollectAttributeKeys().isEmpty()) {
            timeSeriesAggregationQuery.withAttributeCollection(request.getCollectAttributeKeys(),
                request.getCollectAttributesValuesLimit());
        }
        if (request.getIntervalSize() > 0) {
            timeSeriesAggregationQuery.window(request.getIntervalSize());
        }
        if (request.getNumberOfBuckets() != null) {
            timeSeriesAggregationQuery.split(request.getNumberOfBuckets());
        }
        return timeSeriesAggregationQuery.build();
    }

    private TimeSeriesAPIResponse mapToApiResponse(FetchBucketsRequest request, TimeSeriesAggregationResponse response) {
        Map<BucketAttributes, Map<Long, Bucket>> series = response.getSeries();
        long intervalSize = response.getResolution();
        long start = response.getStart();
        long end = response.getEnd();

        List<BucketAttributes> matrixKeys = new ArrayList<>();
        List<List<BucketResponse>> matrix = new ArrayList<>();
        series.keySet().stream().limit(request.getMaxNumberOfSeries()).forEach(key -> {
            Map<Long, Bucket> currentSeries = series.get(key);
            List<BucketResponse> bucketResponses = new ArrayList<>();
            for (long index = start; index < end; index += response.getResolution()) {
                Bucket b = currentSeries.get(index);
                BucketResponse bucketResponse = null;
                if (b != null) {
                    bucketResponse = new BucketResponseBuilder()
                        .withBegin(b.getBegin())
                        .withCount(b.getCount())
                        .withMin(b.getMin())
                        .withMax(b.getMax())
                        .withSum(b.getSum())
                        .withThroughputPerHour(3600 * 1000 * b.getCount() / (b.getEnd() - b.getBegin()))
                        .withPclValues(request.getPercentiles().stream().collect(Collectors.toMap(p -> p, b::getPercentile, (existing, replacement) -> existing)))
                        .withAttributes(b.getAttributes())
                        .build();
                }
                bucketResponses.add(bucketResponse);
            }
            matrix.add(bucketResponses);
            matrixKeys.add(key);
        });

        return new TimeSeriesAPIResponseBuilder()
            .withStart(start)
            .withEnd(end)
            .withInterval(intervalSize)
            .withMatrixKeys(matrixKeys)
            .withMatrix(matrix)
            .withTruncated(series.size() > request.getMaxNumberOfSeries())
            .withCollectionResolution(response.getCollectionResolution())
            .withCollectionIgnoredAttributes(response.getCollectionIgnoredAttributes())
            .withHigherResolutionUsed(response.isHigherResolutionUsed())
            .withTtlCovered(response.isTtlCovered())
            .build();
    }

    private void validateFetchRequest(FetchBucketsRequest request) {
        if (request.getStart() == null || request.getEnd() == null) {
            throw new ControllerServiceException("Start and End parameters must be specified");
        }
        if (request.getStart() > request.getEnd()) {
            throw new ControllerServiceException("Start value must be lower than End");
        }
    }

}

