package step.plugins.measurements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.controller.grid.GridPlugin;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.entities.EntityConstants;
import step.core.metrics.InstrumentType;
import step.core.metrics.MetricSample;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.timeseries.metric.MetricAggregation;
import step.core.timeseries.metric.MetricAggregationType;
import step.core.timeseries.metric.MetricRenderingSettings;
import step.core.timeseries.metric.MetricType;
import step.core.timeseries.metric.MetricTypeAccessor;
import step.engine.plugins.ExecutionEnginePlugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.grid.TokenWrapperState;
import step.grid.client.GridClient;
import step.grid.client.reports.GridReportBuilder;
import step.grid.client.reports.TokenGroupCapacity;

import java.util.*;

import static step.plugins.measurements.MetricsConstants.*;
import static step.plugins.measurements.SamplesExecutionPlugin.*;

@Plugin(dependencies = {GridPlugin.class})
public class SamplesControllerPlugin extends AbstractControllerPlugin {

    private static final Logger logger = LoggerFactory.getLogger(SamplesControllerPlugin.class);

    public static final String EXECUTIONS_DURATION = "executions/duration";
    public static final String EXECUTIONS_COUNT = "executions/count";
    public static final String FAILURE_PERCENTAGE = "executions/failure-percentage";
    public static final String FAILURE_COUNT = "executions/failure-count";
    public static final String FAILURES_COUNT_BY_ERROR_CODE = "executions/failures-count-by-error-code";
    public static final String RESPONSE_TIME = "response-time";
    public static final String THREAD_GROUP = "threadgroup";
    public static final String ERROR_CODE = "errorCode";

    public static String GRID_SAMPLER_NAME = "grid_tokens_sampler";
    public static String GRID_BY_STATE_METRIC_NAME = "grid_tokens_by_state";
    public static String GRID_CAPACITY_METRIC_NAME = "grid_tokens_capacity";
    public static String ReportMeasurementsTableName = "reportMeasurements";
    private MetricSamplerRegistry metricSamplerRegistry;
    private MetricTypeRegistry metricTypeRegistry;

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);

        Collection<Measurement> collection = context.getCollectionFactory().getCollection(EntityConstants.measurements, Measurement.class);
        context.require(TableRegistry.class).register(ReportMeasurementsTableName,
            new Table<>(collection, null, false)
                .withResultItemTransformer((m, session) -> convertToPseudoMeasure(m))
        );

        MetricTypeAccessor metricTypeAccessor = new MetricTypeAccessor(context.getCollectionFactory().getCollection(EntityConstants.metricTypes, MetricType.class));
        context.put(MetricTypeAccessor.class, metricTypeAccessor);

        metricTypeRegistry = new MetricTypeRegistry(metricTypeAccessor);
        context.put(MetricTypeRegistry.class, metricTypeRegistry);
        createOrUpdateMetrics();

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
        configureGridMonitoring(context);

        //Start the metric sampling scheduler
        int interval = context.getConfiguration().getPropertyAsInteger("plugins.measurements.gaugecollector.interval", 15);
        metricSamplerRegistry.start(interval);

        //Start the metric heartbeat scheduler
        int heartbeatIntervalSec = context.getConfiguration().getPropertyAsInteger("plugins.measurements.metricheartbeat.interval", 15);
        MetricHeartbeatRegistry.getInstance().start(heartbeatIntervalSec * 1000L);
    }

    /**
     * This method should be moved to the GridPlugin class but that would currently create circular dependencies. Some classes such as MetricSampler, MetricSamplerRegistry should be move to step.core or similar
     * This method register the collector to monitor the grid usage
     * @param context the controller global context
     */
    private void configureGridMonitoring(GlobalContext context) {
        // make sure we have a grid client in the current context
        if (context.get(GridClient.class) != null) {
            metricSamplerRegistry.registerSampler(GRID_SAMPLER_NAME, new MetricSampler(GRID_SAMPLER_NAME,
                "step grid token usage and capacity") {
                final GridReportBuilder gridReportBuilder = new GridReportBuilder(context.require(GridClient.class));
                @Override
                public List<ControllerMetricSample> collectMetricSamples() {
                    Set<String> tokenAttributeKeys = new HashSet<>(gridReportBuilder.getTokenAttributeKeys());
                    tokenAttributeKeys.removeAll(List.of("$agentid", "$tokenid"));
                    List<TokenGroupCapacity> usageByIdentity = gridReportBuilder.getUsageByIdentity(tokenAttributeKeys);
                    List<ControllerMetricSample> gridMetricSamples = new ArrayList<>();
                    long now = System.currentTimeMillis();
                    for (TokenGroupCapacity tokenGroupCapacity : usageByIdentity) {
                        int capacity = tokenGroupCapacity.getCapacity();
                        Map<String, String> labels = new TreeMap<>(tokenGroupCapacity.getKey());
                        gridMetricSamples.add(new ControllerMetricSample(
                            new MetricSample(now, "grid_tokens_capacity", labels, InstrumentType.GAUGE,
                                1, capacity, capacity, capacity, capacity, null),
                            GRID_CAPACITY_METRIC_NAME));
                        for (TokenWrapperState state : TokenWrapperState.values()) {
                            int valueByState = Objects.requireNonNullElse(tokenGroupCapacity.getCountByState().get(state), 0);
                            TreeMap<String, String> labelsWithState = new TreeMap<>(labels);
                            labelsWithState.put(GRID_TOKEN_STATE.getName(), state.name());
                            gridMetricSamples.add(new ControllerMetricSample(
                                new MetricSample(now, "grid_token_" + state.name(), labelsWithState, InstrumentType.GAUGE,
                                1, valueByState, valueByState, valueByState, valueByState, null),
                                GRID_BY_STATE_METRIC_NAME));
                        }
                    }
                    return gridMetricSamples;
                }
            });
        } else {
            logger.warn("No grid instance found in context, the measurements of the grid token usage will be disabled");
        }
    }

    private void createOrUpdateMetrics() {
        metricTypeRegistry.registerMetricType(new MetricType()
                .setName(EXECUTIONS_COUNT)
                .setDisplayName("Execution count")
                .setDescription("Total number of plan executions ended over the selected time range.")
                .setAttributes(List.of(TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
                .setDefaultAggregation(new MetricAggregation(MetricAggregationType.SUM))
                .setUnit("1")
                .setRenderingSettings(new MetricRenderingSettings()
                ));
        metricTypeRegistry.registerMetricType(new MetricType()
                .setName(EXECUTIONS_DURATION)
                .setDisplayName("Execution duration")
                .setDescription("Wall-clock duration of each plan execution in milliseconds. Can be filtered by simplified result (FAILED/PASSED) or detailed result.")
                .setAttributes(List.of(TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE, EXECUTION_BOOLEAN_RESULT, EXECUTION_RESULT))
                .setDefaultAggregation(new MetricAggregation(MetricAggregationType.AVG))
                .setUnit("ms")
                .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
                // AVG calculation is enough here. the value is either 0 or 100 for each exec.
                .setName(FAILURE_PERCENTAGE)
                .setDisplayName("Execution failure percentage")
                .setDescription("Failure rate of plan executions as a percentage. Each execution contributes 0 (success) or 100 (failure); the average over the selected time range yields the overall failure rate.")
                .setAttributes(List.of(TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
                .setUnit("%")
                .setDefaultAggregation(new MetricAggregation(MetricAggregationType.AVG))
                .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
                .setName(FAILURE_COUNT)
                .setUnit("1")
                .setDisplayName("Execution failure count")
                .setDescription("Number of plan executions that ended with a failure result over the selected time range.")
                .setAttributes(List.of(TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
                .setDefaultAggregation(new MetricAggregation(MetricAggregationType.SUM))
                .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
                .setName(FAILURES_COUNT_BY_ERROR_CODE)
                .setDisplayName("Execution failure count by error code")
                .setDescription("Number of failed executions broken down by error code, allowing identification of the most frequent failure reasons.")
                .setUnit("1")
                .setDefaultGroupingAttributes(List.of(ERROR_CODE_ATTRIBUTE.getName()))
                .setDefaultAggregation(new MetricAggregation(MetricAggregationType.SUM))
                .setAttributes(Arrays.asList(TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE, ERROR_CODE_ATTRIBUTE))
                .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
                .setName(RESPONSE_TIME)
                .setDisplayName("Response time")
                .setDescription("Response time in milliseconds of individual steps or keywords measured during plan execution.")
                .setAttributes(Arrays.asList(STATUS_ATTRIBUTE, TYPE_ATRIBUTE, NAME_ATTRIBUTE, TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
                .setDefaultGroupingAttributes(List.of(NAME_ATTRIBUTE.getName()))
                .setUnit("ms")
                .setDefaultAggregation(new MetricAggregation(MetricAggregationType.AVG))
                .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
                .setName(step.core.metrics.InstrumentType.HISTOGRAM.toLowerCase())
                .setDisplayName("Histogram")
                .setDescription("Custom histogram metrics recorded by keywords in plan executions.")
                .setAttributes(Arrays.asList(TYPE_ATRIBUTE, NAME_ATTRIBUTE, TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
                .setDefaultGroupingAttributes(List.of(NAME_ATTRIBUTE.getName()))
                .setUnit("")
                .setDefaultAggregation(new MetricAggregation(MetricAggregationType.AVG))
                .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
                .setName(step.core.metrics.InstrumentType.GAUGE.toLowerCase())
                .setDisplayName("Gauge")
                .setDescription("Custom gauge metrics recorded by keywords in plan executions, representing instantaneous numeric values.")
                .setAttributes(Arrays.asList(TYPE_ATRIBUTE, NAME_ATTRIBUTE, TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
                .setDefaultGroupingAttributes(List.of(NAME_ATTRIBUTE.getName()))
                .setUnit("1")
                .setDefaultAggregation(new MetricAggregation(MetricAggregationType.AVG))
                .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
                .setName(step.core.metrics.InstrumentType.COUNTER.toLowerCase())
                .setDisplayName("Counter")
                .setDescription("Custom counter metrics recorded by keywords in plan executions, tracking cumulative event counts.")
                .setAttributes(Arrays.asList(TYPE_ATRIBUTE, NAME_ATTRIBUTE, TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
                .setDefaultGroupingAttributes(List.of(NAME_ATTRIBUTE.getName()))
                .setUnit("1")
                .setDefaultAggregation(new MetricAggregation(MetricAggregationType.COUNT))
                .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
                .setName(THREAD_GROUP)
                .setDisplayName("Thread group")
                .setDescription("Number of concurrent virtual users or threads active in a thread group at a given point in time. The max value is the default aggregation.")
                .setAttributes(Arrays.asList(TYPE_ATRIBUTE, NAME_ATTRIBUTE, TASK_ATTRIBUTE, EXECUTION_ATTRIBUTE, PLAN_ATTRIBUTE))
                .setDefaultGroupingAttributes(List.of(NAME_ATTRIBUTE.getName()))
                .setUnit("1")
                .setDefaultAggregation(new MetricAggregation(MetricAggregationType.MAX))
                .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
            .setName(GRID_BY_STATE_METRIC_NAME)
            .setDisplayName("Grid tokens by state")
            .setDescription("Number of grid tokens (agent execution slots) currently in each lifecycle state, broken down by state and agent type.")
            .setAttributes(Arrays.asList(GRID_TOKEN_STATE, GRID_TOKEN_AGENT_TYPE))
            .setDefaultGroupingAttributes(List.of(GRID_TOKEN_STATE.getName(), GRID_TOKEN_AGENT_TYPE.getName()))
            .setUnit("1")
            .setDefaultAggregation(new MetricAggregation(MetricAggregationType.SUM))
            .setRenderingSettings(new MetricRenderingSettings()));
        metricTypeRegistry.registerMetricType(new MetricType()
            .setName(GRID_CAPACITY_METRIC_NAME)
            .setDisplayName("Grid tokens capacity")
            .setDescription("Total number of available grid token slots per agent type, representing the maximum execution concurrency of the grid.")
            .setAttributes(List.of(GRID_TOKEN_AGENT_TYPE))
            .setDefaultGroupingAttributes(List.of(GRID_TOKEN_AGENT_TYPE.getName()))
            .setUnit("1")
            .setDefaultAggregation(new MetricAggregation(MetricAggregationType.SUM))
            .setRenderingSettings(new MetricRenderingSettings()));
    }

    @Override
    public void serverStop(GlobalContext context) {
        metricSamplerRegistry.stop();
        MetricHeartbeatRegistry.getInstance().stop();
    }

    @Override
    public ExecutionEnginePlugin getExecutionEnginePlugin() {
        return new SamplesExecutionPlugin();
    }
}
