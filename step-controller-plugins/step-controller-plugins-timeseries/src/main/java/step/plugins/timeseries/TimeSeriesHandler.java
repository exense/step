package step.plugins.timeseries;

import ch.exense.commons.app.Configuration;
import org.apache.commons.lang3.StringUtils;
import step.controller.services.async.AsyncTaskManager;
import step.controller.services.async.AsyncTaskStatus;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.collections.SearchOrder;
import step.core.collections.filters.Equals;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.deployment.ControllerServiceException;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.TimeSeriesFilterBuilder;
import step.core.timeseries.TimeSeriesIngestionPipeline;
import step.core.timeseries.aggregation.TimeSeriesAggregationPipeline;
import step.core.timeseries.aggregation.TimeSeriesAggregationQuery;
import step.core.timeseries.aggregation.TimeSeriesAggregationQueryBuilder;
import step.core.timeseries.aggregation.TimeSeriesAggregationResponse;
import step.core.timeseries.bucket.Bucket;
import step.core.timeseries.bucket.BucketAttributes;
import step.core.timeseries.query.OQLTimeSeriesFilterBuilder;
import step.plugins.measurements.Measurement;
import step.plugins.measurements.MeasurementPlugin;
import step.plugins.timeseries.api.*;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;

import static step.plugins.timeseries.TimeSeriesControllerPlugin.RESOLUTION_PERIOD_PROPERTY;
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
    private final step.core.collections.Collection<Measurement> measurementCollection;
    private final ExecutionAccessor executionAccessor;
    private final TimeSeries timeSeries;
    private final int resolution;

    public TimeSeriesHandler(int resolution,
                             List<String> timeSeriesAttributes,
                             step.core.collections.Collection<Measurement> measurementCollection,
                             ExecutionAccessor executionAccessor,
                             TimeSeries timeSeries,
                             TimeSeriesAggregationPipeline aggregationPipeline,
                             AsyncTaskManager asyncTaskManager) {
        this.resolution = resolution;
        this.timeSeriesAttributes = timeSeriesAttributes;
        this.measurementCollection = measurementCollection;
        this.aggregationPipeline = aggregationPipeline;
        this.executionAccessor = executionAccessor;
        this.asyncTaskManager = asyncTaskManager;
        this.timeSeries = timeSeries;
        this.attributesWithPrefix = this.timeSeriesAttributes
                .stream()
                .map(x -> ATTRIBUTES_PREFIX + x)
                .collect(Collectors.toList());
    }

    /**
     * This method fetches and group the RAW measurements into a bucket structure.
     */
    public TimeSeriesAPIResponse getMeasurements(FetchBucketsRequest request, Collection<String> fields) {
        step.core.collections.Collection<Bucket> inmemoryBuckets = new InMemoryCollection<>();
        TimeSeries timeSeries = new TimeSeries(inmemoryBuckets, Set.of(), resolution);

        try (TimeSeriesIngestionPipeline ingestionPipeline = timeSeries.newIngestionPipeline(3000)) {
            List<String> standardAttributes = new ArrayList<>(timeSeriesAttributes);
            standardAttributes.addAll(fields.stream().map(attributesPrefixRemoval).collect(Collectors.toList()));
            standardAttributes.addAll(request.getGroupDimensions());
            TimeSeriesBucketingHandler timeSeriesBucketingHandler = new TimeSeriesBucketingHandler(ingestionPipeline, standardAttributes);
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
            measurementCollection.find(filter, searchOrder, null, null, 0).forEach(measurement -> {
                count.increment();
                timeSeriesBucketingHandler.ingestExistingMeasurement(measurement);
            });
        }

        TimeSeriesAggregationPipeline aggregationPipeline = new TimeSeriesAggregationPipeline(inmemoryBuckets, resolution);
        TimeSeriesAggregationQuery query = mapToQuery(request, aggregationPipeline);
        TimeSeriesAggregationResponse response = query.run();

        return mapToApiResponse(request, response);
    }

    public TimeSeriesAPIResponse getBuckets(FetchBucketsRequest request) {
        validateFetchRequest(request);

        OQLVerifyResponse oqlVerifyResponse = this.verifyOql(request.getOqlFilter());
        if (!oqlVerifyResponse.isValid()) {
            throw new ControllerServiceException("Invalid OQL filter");
        } else if (oqlVerifyResponse.hasUnknownFields() || !this.timeSeriesAttributes.containsAll(request.getGroupDimensions())) {
            // if the filter has attributes which are not indexed, switch to RAW measurements
            return this.getMeasurements(request, oqlVerifyResponse.getFields());
        } else {
            TimeSeriesAggregationQuery query = mapToQuery(request, this.aggregationPipeline);
            TimeSeriesAggregationResponse response = query.run();
            return mapToApiResponse(request, response);
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
            Equals measurementFilter = Filters.equals(MeasurementPlugin.ATTRIBUTE_EXECUTION_ID, executionId);
            Measurement firstMeasurement = measurementCollection.find(measurementFilter,
                    new SearchOrder(MeasurementPlugin.BEGIN, 1), 0, 1, 0).findFirst().orElse(null);
            Measurement lastMeasurement = measurementCollection.find(measurementFilter,
                    new SearchOrder(MeasurementPlugin.BEGIN, -1), 0, 1, 0).findFirst().orElse(null);
            if (firstMeasurement != null && lastMeasurement != null) {
                return asyncTaskManager.scheduleAsyncTask(t -> {
                    // the flushing period can be a big value, because we will force flush every time.
                    // we create a new pipeline for every migration
                    try (TimeSeriesIngestionPipeline ingestionPipeline = timeSeries.newIngestionPipeline(3000)) {
                        TimeSeriesBucketingHandler timeSeriesBucketingHandler = new TimeSeriesBucketingHandler(ingestionPipeline, timeSeriesAttributes);
                        LongAdder count = new LongAdder();
                        SearchOrder searchOrder = new SearchOrder("begin", 1);
                        // Iterate over each measurement and ingest it again
                        measurementCollection.find(measurementFilter, searchOrder, null, null, 0).forEach(measurement -> {
                            count.increment();
                            timeSeriesBucketingHandler.ingestExistingMeasurement(measurement);
                        });
                        return new TimeSeriesRebuildResponse(count.longValue());
                    }
                });
            } else {
                throw new ControllerServiceException("No measurement found matching this execution id");
            }
        }
    }

    private TimeSeriesAggregationQuery mapToQuery(FetchBucketsRequest request, TimeSeriesAggregationPipeline pipeline) {
        TimeSeriesAggregationQueryBuilder timeSeriesAggregationQuery = pipeline.newQueryBuilder()
                .range(request.getStart(), request.getEnd())
                .withFilter(Filters.and(
                        Arrays.asList(
                                TimeSeriesFilterBuilder.buildFilter(request.getOqlFilter()),
                                TimeSeriesFilterBuilder.buildFilter(request.getParams()))
                ))
                .withGroupDimensions(request.getGroupDimensions());
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
        List<Long> axis = response.getAxis();
        Long start = axis.get(0);
        Long end = axis.get(axis.size() - 1) + intervalSize;

        List<BucketAttributes> matrixKeys = new ArrayList<>();
        List<List<BucketResponse>> matrix = new ArrayList<>();
        series.keySet().forEach(key -> {
            Map<Long, Bucket> currentSeries = series.get(key);
            List<BucketResponse> bucketResponses = new ArrayList<>();
            axis.forEach(index -> {
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
                            .withPclValues(request.getPercentiles().stream().collect(Collectors.toMap(p -> p, b::getPercentile)))
                            .build();
                }
                bucketResponses.add(bucketResponse);
            });
            matrix.add(bucketResponses);
            matrixKeys.add(key);
        });

        return new TimeSeriesAPIResponseBuilder()
                .withStart(start)
                .withEnd(end)
                .withInterval(intervalSize)
                .withMatrixKeys(matrixKeys)
                .withMatrix(matrix)
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
