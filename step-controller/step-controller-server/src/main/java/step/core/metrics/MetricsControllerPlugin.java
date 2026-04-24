package step.core.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.entities.EntityConstants;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.timeseries.metric.MetricAggregation;
import step.core.timeseries.metric.MetricAggregationType;
import step.core.timeseries.metric.MetricRenderingSettings;
import step.core.timeseries.metric.MetricType;
import step.engine.plugins.ExecutionEnginePlugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;

import java.util.*;

import static step.core.metrics.InstrumentType.COUNTER;
import static step.core.metrics.InstrumentType.GAUGE;
import static step.core.metrics.InstrumentType.HISTOGRAM;
import static step.core.metrics.MetricsConstants.*;
import static step.core.metrics.MetricsExecutionPlugin.*;

@Plugin
public class MetricsControllerPlugin extends AbstractControllerPlugin {

    private static final Logger logger = LoggerFactory.getLogger(MetricsControllerPlugin.class);

    public static final String IS_CONTROLLER_METRIC = "IS_CONTROLLER_METRIC";
    public static final String EXECUTIONS_DURATION = "executions/duration";
    public static final String EXECUTIONS_COUNT = "executions/count";
    public static final String FAILURE_PERCENTAGE = "executions/failure-percentage";
    public static final String FAILURE_COUNT = "executions/failure-count";
    public static final String FAILURES_COUNT_BY_ERROR_CODE = "executions/failures-count-by-error-code";
    public static final String RESPONSE_TIME = "response-time";
    public static final String THREAD_GROUP = "threadgroup";
    public static final String ERROR_CODE = "errorCode";

    public static String ReportMeasurementsTableName = "reportMeasurements";
    private MetricSamplerRegistry metricSamplerRegistry;

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);

        Collection<Measurement> collection = context.getCollectionFactory().getCollection(EntityConstants.measurements, Measurement.class);
        context.require(TableRegistry.class).register(ReportMeasurementsTableName,
            new Table<>(collection, null, false)
                .withResultItemTransformer((m, session) -> convertToPseudoMeasure(m))
        );

        createOrUpdateMetrics(context.require(MetricTypeRegistry.class));

        initMetricSamplingAndHeartbeat(context);
    }

    /*
         This will convert a "full", flattened, measurement back
         to a format that's structurally identical to a measure.
         IOW, the (JSON) serialized result should "almost" be deserializable back to a Measure,
         but it may contain status values that come from a ReportNode instead of a Measure, and
         therefore might be invalid as a Measure.Status enum value. The frontend (who is the
         only intended user of this) knows how to handle these objects.
         */
    private Measurement convertToPseudoMeasure(Measurement in) {
        Measurement out = new Measurement();
        Map<String, Object> data = new HashMap<>();
        for (Map.Entry<String, Object> entry : in.entrySet()) {
            String key = entry.getKey();
            if (MEASURE_FIELDS.contains(key)) {
                if (key.equals(VALUE)) {
                    // special case: map "value" back to "duration"
                    out.put("duration", entry.getValue());
                } else {
                    out.put(key, entry.getValue());
                }
            } else if (!MEASURE_NOT_DATA_KEYS.contains(key)) {
                data.put(key, entry.getValue());
            }
        }
        if (!data.isEmpty()) {
            out.put("data", data);
        }
        return out;
    }

    protected void initMetricSamplingAndHeartbeat(GlobalContext context) {
        metricSamplerRegistry = MetricSamplerRegistry.getInstance();

        //Start the metric sampling scheduler
        int interval = context.getConfiguration().getPropertyAsInteger("plugins.measurements.gaugecollector.interval", 15);
        metricSamplerRegistry.start(interval);

        //Start the metric heartbeat scheduler
        int heartbeatIntervalSec = context.getConfiguration().getPropertyAsInteger("plugins.measurements.metricheartbeat.interval", 15);
        MetricHeartbeatRegistry.getInstance().start(heartbeatIntervalSec * 1000L);
    }

    private void createOrUpdateMetrics(MetricTypeRegistry metricTypeRegistry) {
        metricTypeRegistry.registerMetricType(new MetricType()
            .setName(EXECUTIONS_COUNT)
            .setDisplayName("Execution count")
            .setDescription("Total number of plan executions ended over the selected time range.")
            .setInstrumentType(HISTOGRAM.toLowerCase())//Not a gauge because no values doesn't mean last values still applies and not a counter value can increment and decrement
            .setAttributes(List.of(TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
            .setDefaultAggregation(new MetricAggregation(MetricAggregationType.SUM))
            .setUnit("1")
            .setRenderingSettings(new MetricRenderingSettings()
            ));
        metricTypeRegistry.registerMetricType(new MetricType()
            .setName(EXECUTIONS_DURATION)
            .setDisplayName("Execution duration")
            .setDescription("Wall-clock duration of each plan execution in milliseconds. Can be filtered by simplified result (FAILED/PASSED) or detailed result.")
            .setInstrumentType(HISTOGRAM.toLowerCase())
            .setAttributes(List.of(TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE, EXECUTION_BOOLEAN_RESULT, EXECUTION_RESULT))
            .setDefaultAggregation(new MetricAggregation(MetricAggregationType.AVG))
            .setUnit("ms")
            .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
            // AVG calculation is enough here. the value is either 0 or 100 for each exec.
            .setName(FAILURE_PERCENTAGE)
            .setDisplayName("Execution failure percentage")
            .setDescription("Failure rate of plan executions as a percentage. Each execution contributes 0 (success) or 100 (failure); the average over the selected time range yields the overall failure rate.")
            .setInstrumentType(HISTOGRAM.toLowerCase())//Not a gauge because no values doesn't mean last values still applies and not a counter value can increment and decrement
            .setAttributes(List.of(TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
            .setUnit("%")
            .setDefaultAggregation(new MetricAggregation(MetricAggregationType.AVG))
            .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
            .setName(FAILURE_COUNT)
            .setUnit("1")
            .setDisplayName("Execution failure count")
            .setDescription("Number of plan executions that ended with a failure result over the selected time range.")
            .setInstrumentType(HISTOGRAM.toLowerCase())//Not a gauge because no values doesn't mean last values still applies and not a counter value can increment and decrement
            .setAttributes(List.of(TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
            .setDefaultAggregation(new MetricAggregation(MetricAggregationType.SUM))
            .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
            .setName(FAILURES_COUNT_BY_ERROR_CODE)
            .setDisplayName("Execution failure count by error code")
            .setDescription("Number of failed executions broken down by error code, allowing identification of the most frequent failure reasons.")
            .setInstrumentType(HISTOGRAM.toLowerCase())//Not a gauge because no values doesn't mean last values still applies and not a counter value can increment and decrement
            .setUnit("1")
            .setDefaultGroupingAttributes(List.of(ERROR_CODE_ATTRIBUTE.getName()))
            .setDefaultAggregation(new MetricAggregation(MetricAggregationType.SUM))
            .setAttributes(Arrays.asList(TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE, ERROR_CODE_ATTRIBUTE))
            .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
            .setName(RESPONSE_TIME)
            .setDisplayName("Response time")
            .setDescription("Response time in milliseconds of individual steps or keywords measured during plan execution.")
            .setInstrumentType(HISTOGRAM.toLowerCase())
            .setAttributes(Arrays.asList(STATUS_ATTRIBUTE, TYPE_ATRIBUTE, NAME_ATTRIBUTE, TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
            .setDefaultGroupingAttributes(List.of(NAME_ATTRIBUTE.getName()))
            .setUnit("ms")
            .setDefaultAggregation(new MetricAggregation(MetricAggregationType.AVG))
            .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
            .setName(step.core.metrics.InstrumentType.HISTOGRAM.toLowerCase())
            .setDisplayName("Histogram")
            .setDescription("Custom histogram metrics recorded by keywords in plan executions.")
            .setInstrumentType(HISTOGRAM.toLowerCase())
            .setAttributes(Arrays.asList(TYPE_ATRIBUTE, NAME_ATTRIBUTE, TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
            .setDefaultGroupingAttributes(List.of(NAME_ATTRIBUTE.getName()))
            .setUnit("")
            .setDefaultAggregation(new MetricAggregation(MetricAggregationType.AVG))
            .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
            .setName(step.core.metrics.InstrumentType.GAUGE.toLowerCase())
            .setDisplayName("Gauge")
            .setDescription("Custom gauge metrics recorded by keywords in plan executions, representing instantaneous numeric values.")
            .setInstrumentType(GAUGE.toLowerCase())
            .setAttributes(Arrays.asList(TYPE_ATRIBUTE, NAME_ATTRIBUTE, TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
            .setDefaultGroupingAttributes(List.of(NAME_ATTRIBUTE.getName()))
            .setUnit("1")
            .setDefaultAggregation(new MetricAggregation(MetricAggregationType.AVG))
            .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
            .setName(COUNTER.toLowerCase())
            .setDisplayName("Counter")
            .setDescription("Custom counter metrics recorded by keywords in plan executions, tracking cumulative event counts.")
            .setInstrumentType(COUNTER.toLowerCase())
            .setAttributes(Arrays.asList(TYPE_ATRIBUTE, NAME_ATTRIBUTE, TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
            .setDefaultGroupingAttributes(List.of(NAME_ATTRIBUTE.getName()))
            .setUnit("1")
            .setDefaultAggregation(new MetricAggregation(MetricAggregationType.SUM))
            .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
            .setName(THREAD_GROUP)
            .setDisplayName("Thread group")
            .setDescription("Number of concurrent virtual users or threads active in a thread group at a given point in time. The max value is the default aggregation.")
            .setInstrumentType(GAUGE.toLowerCase())
            .setAttributes(Arrays.asList(TYPE_ATRIBUTE, NAME_ATTRIBUTE, TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
            .setDefaultGroupingAttributes(List.of(NAME_ATTRIBUTE.getName()))
            .setUnit("1")
            .setDefaultAggregation(new MetricAggregation(MetricAggregationType.MAX))
            .setRenderingSettings(new MetricRenderingSettings()));
    }

    @Override
    public void serverStop(GlobalContext context) {
        metricSamplerRegistry.stop();
        MetricHeartbeatRegistry.getInstance().stop();
    }

    @Override
    public ExecutionEnginePlugin getExecutionEnginePlugin() {
        return new MetricsExecutionPlugin();
    }
}
