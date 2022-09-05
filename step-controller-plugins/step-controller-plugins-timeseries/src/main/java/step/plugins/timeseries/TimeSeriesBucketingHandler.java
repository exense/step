package step.plugins.timeseries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.timeseries.BucketAttributes;
import step.core.timeseries.TimeSeriesIngestionPipeline;
import step.plugins.measurements.Measurement;
import step.plugins.measurements.MeasurementHandler;

import java.util.List;
import java.util.Map;

public class TimeSeriesBucketingHandler implements MeasurementHandler {

    private static final String THREAD_GROUP_MEASUREMENT_TYPE = "threadgroup";
    private static final String METRIC_TYPE_KEY = "metricType";
    private static final String METRIC_TYPE_RESPONSE_TIME = "response-time";
    private static final String METRIC_TYPE_SAMPLER = "sampler";

    private static final Logger logger = LoggerFactory.getLogger(TimeSeriesBucketingHandler.class);

    private final TimeSeriesIngestionPipeline ingestionPipeline;

    public TimeSeriesBucketingHandler(TimeSeriesIngestionPipeline ingestionPipeline) {
        this.ingestionPipeline = ingestionPipeline;
    }

    @Override
    public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {

    }

    @Override
    public void processMeasurements(List<Measurement> measurements) {
        measurements.forEach(measurement -> {
            long begin = measurement.getBegin();
            long value = measurement.getValue();
            BucketAttributes bucketAttributes = new BucketAttributes(measurement);
            removeKeys(bucketAttributes,"rnId", "origin", "planId", "agentUrl", "id", "begin", "value");
            bucketAttributes.put(METRIC_TYPE_KEY, METRIC_TYPE_RESPONSE_TIME);
            // custom fields include all the attributes like execId and planId
            this.ingestionPipeline.ingestPoint(bucketAttributes, begin, value);
        });
    }

    private void removeKeys(Map<String, Object> map, String... attributes) {
        for (String attribute : attributes) {
            map.remove(attribute);
        }
    }

    @Override
    public void processGauges(List<Measurement> measurements) {
        measurements.forEach(measurement -> {
            if (measurement != null && measurement.getType().equals(THREAD_GROUP_MEASUREMENT_TYPE)) {
                BucketAttributes bucketAttributes = new BucketAttributes(measurement);
                bucketAttributes.remove("taskId");
                bucketAttributes.remove("type");
                bucketAttributes.put(METRIC_TYPE_KEY, METRIC_TYPE_SAMPLER);
                this.ingestionPipeline.ingestPoint(bucketAttributes, measurement.getBegin(), measurement.getValue());
            }
        });
    }

    @Override
    public void afterExecutionEnd(ExecutionContext context) {
    }
}
