package step.plugins.timeseries;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import step.controller.services.async.AsyncTaskManager;
import step.core.collections.Collection;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.execution.model.Execution;
import step.core.execution.model.InMemoryExecutionAccessor;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.aggregation.TimeSeriesAggregationPipeline;
import step.core.timeseries.bucket.Bucket;
import step.core.timeseries.bucket.BucketAttributes;
import step.plugins.measurements.Measurement;
import step.plugins.timeseries.api.BucketResponse;
import step.plugins.timeseries.api.FetchBucketsRequest;
import step.plugins.timeseries.api.OQLVerifyResponse;
import step.plugins.timeseries.api.TimeSeriesAPIResponse;

import java.util.*;

import static step.plugins.timeseries.TimeSeriesExecutionPlugin.TIMESERIES_FLAG;

public class TimeSeriesHandlerTest {

    private static final List<String> TS_ATTRIBUTES = Arrays.asList("key", "field1", "field2", "field3");
    private static TimeSeriesHandler handler;

    private static InMemoryExecutionAccessor executionAccessor;
    private static Collection<Bucket> bucketsCollection;
    private static Collection<Measurement> measurementsCollection;

    @BeforeClass
    public static void init() {
        measurementsCollection = new InMemoryCollection<>();
        executionAccessor = new InMemoryExecutionAccessor();
        bucketsCollection = new InMemoryCollection<>();
        TimeSeries timeSeries = new TimeSeries(bucketsCollection, Set.of(), 10);
        TimeSeriesAggregationPipeline aggregationPipeline = timeSeries.getAggregationPipeline();
        AsyncTaskManager asyncTaskManager = new AsyncTaskManager();
        handler = new TimeSeriesHandler(TS_ATTRIBUTES, measurementsCollection, executionAccessor, timeSeries, aggregationPipeline, asyncTaskManager);
    }

    @Test
    public void verifyEmptyOQLTest() {
        String oql = "";
        OQLVerifyResponse response = handler.verifyOql(oql);
        Assert.assertTrue(response.isValid());
        Assert.assertFalse(response.hasUnknownFields());
        Assert.assertTrue(response.getFields().isEmpty());
    }

    @Test
    public void invalidOqlTest() {
        String oql = "(abcdef = 1234)dsfsdfds";
        OQLVerifyResponse response = handler.verifyOql(oql);
        Assert.assertFalse(response.isValid());
        Assert.assertFalse(response.hasUnknownFields());
        Assert.assertTrue(response.getFields().isEmpty());
    }

    @Test
    public void oqlKnownFieldsTest() {
        String oql = "(attributes.field1 = attributes.abcd and attributes.field2 = abcd)";
        OQLVerifyResponse response = handler.verifyOql(oql);
        Assert.assertTrue(response.isValid());
        Assert.assertFalse(response.hasUnknownFields());
        Assert.assertTrue(response.getFields().containsAll(Arrays.asList("attributes.field1", "attributes.field2")));
    }

    @Test
    public void oqlUnknownFieldsTest() {
        String oql = "(attributes.field3 = 1234) and attributes.field4 = 456";
        OQLVerifyResponse response = handler.verifyOql(oql);
        Assert.assertTrue(response.isValid());
        Assert.assertTrue(response.hasUnknownFields());
        Assert.assertTrue(response.getFields().containsAll(Arrays.asList("attributes.field3", "attributes.field4")));
    }

    @Test
    public void checkIfTimeSeriesExistsTest() {
        Execution noTimeseriesExecution = new Execution();
        executionAccessor.save(noTimeseriesExecution);
        Execution execution = new Execution();
        executionAccessor.save(execution);
        Assert.assertFalse(handler.timeSeriesIsBuilt(noTimeseriesExecution.getId().toString()));
        Assert.assertFalse(handler.timeSeriesIsBuilt(execution.getId().toString()));
        execution.addCustomField(TIMESERIES_FLAG, true);
        executionAccessor.save(execution);
        Assert.assertTrue(handler.timeSeriesIsBuilt(execution.getId().toString()));
    }

    @Test
    public void fetchEmptyBucketsTest() {
        FetchBucketsRequest request = new FetchBucketsRequest();
        request.setStart(0);
        request.setEnd(9);
        request.setGroupDimensions(Collections.emptySet());
        request.setNumberOfBuckets(1);
        request.setPercentiles(Arrays.asList(10, 20, 50));
        request.setOqlFilter("");
        request.setParams(Map.of("eId", "abc"));
        TimeSeriesAPIResponse response = handler.getBuckets(request);
        Assert.assertEquals(0, response.getStart());
        Assert.assertEquals(10, response.getEnd());
        Assert.assertTrue(response.getMatrix().isEmpty());
        Assert.assertTrue(response.getMatrixKeys().isEmpty());
    }

    private List<Bucket> generateBuckets(String key, int bucketsCount) {
        List<Bucket> buckets = new ArrayList<>();
        for (int i = 0; i < bucketsCount; i++) {
            Bucket bucket = new Bucket();
            bucket.setBegin(i * 1000L);
            bucket.setEnd(bucket.getBegin() + 1);
            int count = RandomUtils.nextInt();
            bucket.setCount(count);
            bucket.setSum(count * 3L);
            BucketAttributes attributes = new BucketAttributes();
            attributes.put("key", key);
            bucket.setAttributes(attributes);
            bucket.setDistribution(Map.of(10L, 10L, 30L, 30L, 50L, 50L, 70L, 70L));
            buckets.add(bucket);
        }
        return buckets;
    }

    @Test
    public void fetchShrinkBucketsTest() {
        String key = RandomStringUtils.randomAlphabetic(5);
        int bucketsCount = 10;
        List<Bucket> buckets = generateBuckets(key, bucketsCount);
        bucketsCollection.save(buckets);
        FetchBucketsRequest request = new FetchBucketsRequest();
        request.setStart(0);
        request.setEnd(bucketsCount * 1000);
        request.setGroupDimensions(Collections.emptySet());
        request.setNumberOfBuckets(1);
        request.setPercentiles(Arrays.asList(10, 20, 50));
        request.setOqlFilter("");
        request.setParams(Map.of("key", key));

        TimeSeriesAPIResponse response = handler.getBuckets(request);
        Assert.assertEquals(0, response.getStart());
        Assert.assertEquals(bucketsCount * 1000, response.getEnd());
        Assert.assertEquals(1, response.getMatrix().size());
        Assert.assertEquals(1, response.getMatrixKeys().size());
        Assert.assertTrue(response.getMatrixKeys().get(0).isEmpty());

        long sumValue = 0;
        long countValue = 0;
        for (Bucket bucket : buckets) {
            sumValue += bucket.getSum();
            countValue += bucket.getCount();
        }
        ;
        BucketResponse aggregatedBucket = response.getMatrix().get(0).get(0);
        Assert.assertEquals(sumValue, aggregatedBucket.getSum());
        Assert.assertEquals(countValue, aggregatedBucket.getCount());
        Assert.assertEquals(3, aggregatedBucket.getPclValues().size());
    }

    @Test
    public void fetchMultipleBucketsTest() {
        String key = RandomStringUtils.randomAlphabetic(5);
        int bucketsCount = 10;
        List<Bucket> buckets = generateBuckets(key, bucketsCount);
        bucketsCollection.save(buckets);
        FetchBucketsRequest request = new FetchBucketsRequest();
        request.setStart(0);
        request.setEnd(bucketsCount * 1000);
        request.setGroupDimensions(Collections.emptySet());
        int responseBucketsCount = bucketsCount / 2;
        request.setNumberOfBuckets(responseBucketsCount); // 5
        request.setPercentiles(Arrays.asList(10, 20, 50));
        request.setOqlFilter("key = " + key);

        TimeSeriesAPIResponse response = handler.getBuckets(request);
        Assert.assertEquals(0, response.getStart());
        Assert.assertEquals(bucketsCount + 1000, response.getEnd());
        Assert.assertEquals(responseBucketsCount, response.getMatrix().size());
        Assert.assertEquals(responseBucketsCount, response.getMatrixKeys().size());
        Assert.assertTrue(response.getMatrixKeys().get(0).isEmpty());

        long sumValue = 0;
        long countValue = 0;
        for (Bucket bucket : buckets) {
            sumValue += bucket.getSum();
            countValue += bucket.getCount();
        }
        ;
        long responseTotalSum = 0;
        long responseTotalCount = 0;
        for (BucketResponse b : response.getMatrix().get(0)) {
            responseTotalSum += b.getSum();
            countValue += b.getCount();
        }
        BucketResponse aggregatedBucket = response.getMatrix().get(0).get(0);
        Assert.assertEquals(sumValue, responseTotalSum);
        Assert.assertEquals(countValue, responseTotalCount);
        Assert.assertEquals(3, aggregatedBucket.getPclValues().size());
    }

    @Test
    public void fetchRawMeasurementsTest() {
        String key = RandomStringUtils.randomAlphabetic(5);
        int bucketsCount = 10;
        int responseBucketsCount = bucketsCount / 2;
        List<Bucket> buckets = generateBuckets(key, bucketsCount);
        bucketsCollection.save(buckets);
        FetchBucketsRequest request = new FetchBucketsRequest();
        request.setStart(0);
        request.setEnd(100_000);
        request.setGroupDimensions(Collections.emptySet());

        request.setNumberOfBuckets(responseBucketsCount); // 5
        request.setPercentiles(Arrays.asList(10, 20, 50));
        request.setOqlFilter("attributes.unknownKey = " + key); // this is not a known field, so it will fall over on RAW data.

        TimeSeriesAPIResponse response = handler.getBuckets(request);
        Assert.assertEquals(0, response.getMatrix().size()); // we don't have measurements with unknown key
        Assert.assertEquals(0, response.getMatrixKeys().size());

        List<Measurement> measurements = generateMeasurements(100, "unknownKey", key);
        measurementsCollection.save(measurements);
        response = handler.getBuckets(request);
        Assert.assertEquals(0, response.getMatrix().size()); // we don't have measurements with unknown key
        Assert.assertEquals(0, response.getMatrixKeys().size());

    }

    @Test
    public void bucketsThroughputTest() {
        String key = RandomStringUtils.randomAlphabetic(5);
        int bucketsCount = 10;
        int responseBucketsCount = bucketsCount / 2;
        List<Bucket> buckets = generateBuckets(key, bucketsCount);
        bucketsCollection.save(buckets);
        FetchBucketsRequest request = new FetchBucketsRequest();
        request.setStart(0);
        request.setEnd(bucketsCount * 1000);
        request.setGroupDimensions(Collections.emptySet());

        request.setNumberOfBuckets(responseBucketsCount); // 5
        request.setPercentiles(Arrays.asList(10, 20, 50));
        request.setOqlFilter("attributes.key = " + key);

        TimeSeriesAPIResponse response = handler.getBuckets(request);
        Assert.assertEquals(1, response.getMatrix().size());
        Assert.assertEquals(responseBucketsCount, response.getMatrix().get(0).size());
        Assert.assertEquals(1, response.getMatrixKeys().size());
        Assert.assertEquals(responseBucketsCount, response.getMatrixKeys().get(0).size());
        response.getMatrix().get(0).forEach(b -> {
            Assert.assertEquals(b.getThroughputPerHour(), 3600 * 1000 * b.getCount() / (b.getBegin() + 1000 - b.getBegin()));
        });
    }

    private List<Measurement> generateMeasurements(int count, String keyAttribute, String keyValue) {
        List<Measurement> measurements = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Measurement measurement = new Measurement();
            measurement.put(keyAttribute, keyValue);
            measurement.setBegin(i + 1000);
            measurement.setType("test-measurement");
            measurement.setValue(RandomUtils.nextLong());
            measurements.add(measurement);
        }
        return measurements;
    }


}
