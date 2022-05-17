package step.plugins.timeseries;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.timeseries.TimeSeriesIngestionPipeline;
import step.plugins.measurements.Measurement;
import step.plugins.measurements.MeasurementHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeSeriesBucketingHandler implements MeasurementHandler {

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
            // custom fields include all the attributes like execId and planId
            this.ingestionPipeline.ingestPoint(measurement.getCustomFields(), measurement.getBegin(), measurement.getValue());
        });
        System.out.println("Received some measurements: " + measurements.size());
    }

    @Override
    public void processGauges(List<Measurement> measurements) {
    }

    @Override
    public void afterExecutionEnd(ExecutionContext context) {
        System.out.println("Execution ended");
    }
}
