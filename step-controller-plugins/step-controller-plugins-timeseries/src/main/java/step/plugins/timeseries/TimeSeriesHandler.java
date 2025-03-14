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
import step.core.timeseries.ingestion.TimeSeriesIngestionPipeline;
import step.core.timeseries.query.OQLTimeSeriesFilterBuilder;
import step.plugins.measurements.Measurement;
import step.plugins.measurements.MeasurementPlugin;
import step.plugins.timeseries.api.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static step.plugins.measurements.MeasurementPlugin.ATTRIBUTE_EXECUTION_ID;
import static step.plugins.timeseries.TimeSeriesExecutionPlugin.TIMESERIES_FLAG;

public class TimeSeriesHandler {

    private static final String ATTRIBUTES_PREFIX = "attributes.";
    private static final String METRIC_TYPE_ATTRIBUTE = "metricType";
    private static final String TIMESTAMP_ATTRIBUTE = "begin";
    private static final List<String> MEASUREMENTS_FILTER_IGNORE_ATTRIBUTES = Arrays.asList(METRIC_TYPE_ATTRIBUTE);
    private static final Function<String, String> attributesPrefixRemoval = (attribute) -> {
        if (attribute.startsWith(ATTRIBUTES_PREFIX)) {
            return attribute.replaceFirst(ATTRIBUTES_PREFIX, "");
        } else {
            return attribute;
        }
    };
    private final List<String> attributesWithPrefix;

    private final List<String> timeSeriesAttributes;
    private final AsyncTaskManager asyncTaskManager;
    private final TimeSeriesAggregationPipeline aggregationPipeline;
    private final TimeSeriesAggregationPipeline reportNodeAggregationPipeline;
    private final step.core.collections.Collection<Measurement> measurementCollection;
    private final ExecutionAccessor executionAccessor;
    private final TimeSeries timeSeries;
    private final int resolution;
    private final int samplingLimit;

    public TimeSeriesHandler(int resolution,
                             List<String> timeSeriesAttributes,
                             step.core.collections.Collection<Measurement> measurementCollection,
                             ExecutionAccessor executionAccessor,
                             TimeSeries timeSeries,
                             ReportNodeTimeSeries reportNodeTimeSeries,
                             AsyncTaskManager asyncTaskManager,
                             int samplingLimit) {
        this.resolution = resolution;
        this.timeSeriesAttributes = timeSeriesAttributes;
        this.measurementCollection = measurementCollection;
        this.aggregationPipeline = timeSeries.getAggregationPipeline();
        this.reportNodeAggregationPipeline = reportNodeTimeSeries.getTimeSeries().getAggregationPipeline();
        this.executionAccessor = executionAccessor;
        this.asyncTaskManager = asyncTaskManager;
        this.timeSeries = timeSeries;
        this.samplingLimit = samplingLimit;
        this.attributesWithPrefix = this.timeSeriesAttributes
                .stream()
                .map(x -> ATTRIBUTES_PREFIX + x)
                .collect(Collectors.toList());
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

            List<String> standardAttributes = new ArrayList<>(timeSeriesAttributes);
            standardAttributes.addAll(fields.stream().map(attributesPrefixRemoval).collect(Collectors.toList()));
            standardAttributes.addAll(request.getGroupDimensions());
            TimeSeriesBucketingHandler timeSeriesBucketingHandler = new TimeSeriesBucketingHandler(timeSeries, standardAttributes);
            LongAdder count = new LongAdder();
            ArrayList<Filter> timestampClauses = new ArrayList<>(List.of(Filters.empty()));
            if (request.getStart() != null) {
                timestampClauses.add(Filters.gte(TIMESTAMP_ATTRIBUTE, request.getStart()));
            }
            if (request.getEnd() != null) {
                timestampClauses.add(Filters.lt(TIMESTAMP_ATTRIBUTE, request.getEnd()));
            }
            Filter filter = Filters.and(Arrays.asList(
                    Filters.and(timestampClauses),
                    OQLTimeSeriesFilterBuilder.getFilter(request.getOqlFilter(), attributesPrefixRemoval, MEASUREMENTS_FILTER_IGNORE_ATTRIBUTES)
            ));
            SearchOrder searchOrder = new SearchOrder(TIMESTAMP_ATTRIBUTE, 1);
            // Iterate over each measurement and ingest it again
            try (Stream<Measurement> stream = measurementCollection.findLazy(filter, searchOrder, null, null, 0)) {
                stream.forEach(measurement -> {
                    count.increment();
                    timeSeriesBucketingHandler.ingestExistingMeasurement(measurement);
                });
            }
            timeSeriesBucketingHandler.flush();
            TimeSeriesAggregationPipeline aggregationPipeline = timeSeries.getAggregationPipeline();
            TimeSeriesAggregationQuery query = mapToQuery(request, aggregationPipeline);
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
                if (Objects.equals(key, MeasurementPlugin.BEGIN)
                        || Objects.equals(key, MeasurementPlugin.VALUE)
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
        int calculatedResolution = (int) Math.floor((request.getEnd() - request.getStart()) / nbBuckets);
        return Math.max(resolution, calculatedResolution);
    }

    public TimeSeriesAPIResponse getTimeSeries(FetchBucketsRequest request) {
        validateFetchRequest(request);
        TimeSeriesAggregationQuery query = mapToQuery(request, this.aggregationPipeline);
        TimeSeriesAggregationResponse response = this.aggregationPipeline.collect(query);
        return mapToApiResponse(request, response);
    }

    public TimeSeriesAPIResponse getReportNodeTimeSeries(FetchBucketsRequest request) {
        validateFetchRequest(request);
        TimeSeriesAggregationQuery query = mapToQuery(request, this.reportNodeAggregationPipeline);
        TimeSeriesAggregationResponse response = this.reportNodeAggregationPipeline.collect(query);
        return mapToApiResponse(request, response);
    }

    /**
     * Gets the time series according to the provided request.
     * If the time series doesn't exist for the provided request, builds it based on the raw measurements
     *
     * @param request
     * @return
     */
    public TimeSeriesAPIResponse getOrBuildTimeSeries(FetchBucketsRequest request) {
        validateFetchRequest(request);

        OQLVerifyResponse oqlVerifyResponse = this.verifyOql(request.getOqlFilter());
        if (!oqlVerifyResponse.isValid()) {
            throw new ControllerServiceException("Invalid OQL filter");
        } else if (oqlVerifyResponse.hasUnknownFields() || !this.timeSeriesAttributes.containsAll(request.getGroupDimensions())) {
            // if the filter has attributes which are not indexed, switch to RAW measurements
            return this.getTimeSeriesFromRawMeasurements(request, oqlVerifyResponse.getFields());
        } else {
            return getTimeSeries(request);
        }
    }

    public OQLVerifyResponse verifyOql(String oql) {

        boolean isValid = true;
        boolean hasUnknownFields = false;
        Set<String> oqlAttributes = Collections.emptySet();
        if (StringUtils.isNotEmpty(oql)) {
            try {
                oqlAttributes = new HashSet<>(OQLTimeSeriesFilterBuilder.getFilterAttributes(oql));
                hasUnknownFields = !attributesWithPrefix.containsAll(oqlAttributes);
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
        measurementCollection.find(filter, null, 0, samplingLimit, 0).forEach(measurement -> {
            fields.addAll(measurement.keySet());
        });
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
                    new SearchOrder(MeasurementPlugin.BEGIN, 1), 0, 1, 0).findFirst().orElse(null);
            Measurement lastMeasurement = measurementCollection.find(measurementFilter,
                    new SearchOrder(MeasurementPlugin.BEGIN, -1), 0, 1, 0).findFirst().orElse(null);
            if (firstMeasurement != null && lastMeasurement != null) {
                return asyncTaskManager.scheduleAsyncTask(t -> {
                    // the flushing period can be a big value, because we will force flush every time.
                    TimeSeriesBucketingHandler timeSeriesBucketingHandler = new TimeSeriesBucketingHandler(timeSeries, timeSeriesAttributes);
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

    private TimeSeriesAggregationQuery mapToQuery(FetchBucketsRequest request, TimeSeriesAggregationPipeline pipeline) {
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
                    Objects.requireNonNullElse(request.getCollectAttributesValuesLimit(), 0));
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

