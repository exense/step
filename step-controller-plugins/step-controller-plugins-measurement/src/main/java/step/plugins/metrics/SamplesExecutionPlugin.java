package step.plugins.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.reports.CallFunctionReportNode;
import step.artefacts.reports.TestCaseReportNode;
import step.artefacts.reports.ThreadReportNode;
import step.automation.packages.AutomationPackage;
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.AbstractTrackedObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.type.ExecutionTypeManager;
import step.core.metrics.CounterMetric;
import step.core.metrics.GaugeMetric;
import step.core.metrics.HistogramMetric;
import step.core.metrics.InstrumentType;
import step.core.metrics.MetricSample;
import step.core.plans.Plan;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.reports.Measure;
import step.core.scheduler.ExecutiontTaskParameters;
import step.core.views.ViewManager;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.functions.Function;
import step.functions.handler.MeasureTypes;
import step.livereporting.LiveReportingPlugin;
import step.plugins.views.functions.ErrorDistribution;
import step.plugins.views.functions.ErrorDistributionView;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static step.automation.packages.AutomationPackageEntity.AUTOMATION_PACKAGE_ID;
import static step.plugins.metrics.MetricsConstants.EXECUTION_BOOLEAN_RESULT;
import static step.plugins.metrics.MetricsConstants.EXECUTION_RESULT;
import static step.plugins.metrics.SamplesControllerPlugin.*;


@Plugin(dependencies = LiveReportingPlugin.class)
@IgnoreDuringAutoDiscovery
public class SamplesExecutionPlugin extends AbstractExecutionEnginePlugin {

    public static final String ATTRIBUTE_EXECUTION_ID = "eId";
    public static final String TYPE_CUSTOM = "custom";
    public static final String TYPE = "type";
    public static final String NAME = "name";
    public static final String BEGIN = "begin";
    public static final String VALUE = "value";
    public static final String RN_ID = "rnId";
    public static final String STATUS = "status";
    public static final String RN_STATUS = "rnStatus";
    public static final String AGENT_URL = "agentUrl";
    public static final String ORIGIN = "origin";
    public static final String TASK_ID = "taskId";
    public static final String SCHEDULE_ID = "scheduleId"; //only use by prometheus handler for now
    public static final String PLAN_ID = "planId";
    public static final String PLAN = "plan";
    public static final String SCHEDULE = "schedule";
    public static final String TEST_CASE = "testcase";
    public static final String EXECUTION_DESCRIPTION = "execution";
    public static final String PROJECT = "project";
    public static final String CTX_SCHEDULER_TASK_ID = "$schedulerTaskId";
    public static final String CTX_SCHEDULE_NAME = "$scheduleName";
    public static final String CTX_EXECUTION_DESCRIPTION = "$executionDescription";
    private static final Logger logger = LoggerFactory.getLogger(SamplesExecutionPlugin.class);
    private static final List<MetricSamplesHandler> SAMPLES_HANDLERS = new ArrayList<>();
    public static final String CTX_GENERATE_EXECUTION_METRICS = "$generateExecutionMetrics";
    public static final String CTX_ADDITIONAL_ATTRIBUTES = "$additionalAttributes";

    // These are used by the MeasurementControllerPlugin to "reconstruct" measures from measurements, and indicate the
    // "internal" fields which should NOT be added to the measure data field. Keep this in sync with the fields defined above.
    static final Set<String> MEASURE_NOT_DATA_KEYS = java.util.Set.of("_id", PROJECT, "projectName", ATTRIBUTE_EXECUTION_ID, RN_ID,
        ORIGIN, RN_STATUS, PLAN_ID, PLAN, AGENT_URL, TASK_ID, SCHEDULE, TEST_CASE, EXECUTION_DESCRIPTION);
    // Same use, but for defining which fields SHOULD be directly copied to the top-level fields of a measure.
    static final Set<String> MEASURE_FIELDS = java.util.Set.of(NAME, BEGIN, VALUE, STATUS);

    // Tracks the live thread count per execution+threadGroup key ("execId|threadGroupName").
    // Incremented/decremented by processThreadReportNode; cleaned up in afterExecutionEnd.
    private final ConcurrentHashMap<String, AtomicLong> threadGroupCounts = new ConcurrentHashMap<>();
    private String controllerUrl;

    public SamplesExecutionPlugin() {
    }

    public static synchronized void registerSamplesHandlers(MetricSamplesHandler handler) {
        SAMPLES_HANDLERS.add(handler);
    }

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext executionEngineContext) {
        controllerUrl = Objects.requireNonNullElse(parentContext.getControllerUrl(), "");
    }

    @Override
    public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
        //Skip executions for which metrics are not meant to be generated (i.e. assertion plans)
        //Unfortunately execution type is not set in initialize context as it is set by the AssertionPlanHandler
        //TODO find a better implementation
        Execution execution = executionContext.getExecutionAccessor().get(executionContext.getExecutionId());
        boolean generateExecutionMetrics = (execution.getExecutionParameters() == null ||
            execution.getExecutionParameters().getCustomField("assertionPlan") == null);
        executionContext.put(CTX_GENERATE_EXECUTION_METRICS, generateExecutionMetrics);
        if (generateExecutionMetrics) {
            //Cache execution metadata in execution context
            String description = execution.getDescription();
            if (description != null) {
                // Executions triggered via REST can have no description at that stage. As a quickfix we check this here to avoid NPE
                // TODO we should set this after the description is updated in ExecutionEngineRunner
                executionContext.put(CTX_EXECUTION_DESCRIPTION, description);
            }
            ExecutiontTaskParameters executiontTaskParameters = execution.getExecutiontTaskParameters();
            if (executiontTaskParameters != null) {
                executionContext.put(CTX_SCHEDULER_TASK_ID, executiontTaskParameters.getId().toHexString());
                String scheduleName = Objects.requireNonNullElse(executiontTaskParameters.getAttribute(AbstractOrganizableObject.NAME), "");
                executionContext.put(CTX_SCHEDULE_NAME, scheduleName);
            }
            TreeMap<String, String> additionalAttributes = Objects.requireNonNullElse(executionContext.getObjectEnricher().getAdditionalAttributes(), new TreeMap<>());
            executionContext.put(CTX_ADDITIONAL_ATTRIBUTES, additionalAttributes);

            for (MetricSamplesHandler metricSamplesHandler : SamplesExecutionPlugin.SAMPLES_HANDLERS) {
                metricSamplesHandler.initializeExecutionContext(executionEngineContext, executionContext);
            }
        }
    }

    private boolean generateMetrics(ExecutionContext executionContext) {
        return (boolean) executionContext.require(CTX_GENERATE_EXECUTION_METRICS);
    }

    @Override
    public void beforeReportNodeExecution(ExecutionContext context, ReportNode node) {
        if (generateMetrics(context) && node instanceof ThreadReportNode) {
            processThreadReportNode(context, (ThreadReportNode) node, true);
        }
    }

    @Override
    public void beforeFunctionExecution(ExecutionContext context, ReportNode node, Function function) {
        LiveReportingPlugin.getLiveReportingContext(context).registerListener(measures -> {
            List<Measurement> measurements = measures.stream().map(m -> createMeasurement(context, m, (CallFunctionReportNode) node)).collect(Collectors.toList());
            processMeasurements(measurements);
        });
        LiveReportingPlugin.getLiveReportingContext(context).registerMetricListener(snapshots -> {
            List<ExecutionMetricSample> executionMetricSamples = snapshots.stream()
                .map(s -> createExecutionMetricSample(context, s, (CallFunctionReportNode) node, null))
                .collect(Collectors.toList());
            processMetrics(executionMetricSamples);
        });
    }

    private void processThreadReportNode(ExecutionContext context, ThreadReportNode node, boolean inc) {
        String execId = context.getExecutionId();
        String threadGroupName = node.getThreadGroupName();
        AtomicLong counter = threadGroupCounts.computeIfAbsent(execId + "|" + threadGroupName, k -> new AtomicLong(0));
        long count = inc ? counter.incrementAndGet() : Math.max(0, counter.decrementAndGet());

        String scheduleId = Objects.requireNonNullElse((String) context.get(CTX_SCHEDULER_TASK_ID), "");
        String schedule = Objects.requireNonNullElse((String) context.get(CTX_SCHEDULE_NAME), "");
        String planId = context.getPlan().getId().toString();
        String plan = Objects.requireNonNullElse(context.getPlan().getAttribute(AbstractOrganizableObject.NAME), "");
        String execution = Objects.requireNonNullElse((String) context.get(CTX_EXECUTION_DESCRIPTION), "");
        @SuppressWarnings("unchecked")
        Map<String, String> additionalAttributes = (Map<String, String>) context.get(CTX_ADDITIONAL_ATTRIBUTES);

        MetricSample sample = new MetricSample(
            System.currentTimeMillis(), threadGroupName,
            Map.of(TYPE, THREAD_GROUP),
            InstrumentType.GAUGE,
            1, count, count, count, count, null);

        ExecutionMetricSample stepSample = new ExecutionMetricSample(
            sample, execId, node.getId().toString(),
            planId, plan, scheduleId, schedule, execution,
            null, null, additionalAttributes, THREAD_GROUP);

        processMetrics(List.of(stepSample));
        // Thread group metrics span the whole execution lifetime (unlike keyword metrics),
        // so they are eligible for heartbeat re-emission when no new value arrives.
        MetricHeartbeatRegistry.getInstance().update(stepSample);
    }

    private Measurement transformToMeasurement(ExecutionContext executionContext, ReportNode node) {
        Measurement measurement = initMeasurement(executionContext);
        measurement.setName(node.getName());
        measurement.setValue(node.getDuration());
        measurement.setBegin(node.getExecutionTime());
        enrichWithNodeAttributes(measurement, node);
        enrichWithAdditionalAttributes(measurement, executionContext);
        AbstractArtefact artefactInstance = node.getArtefactInstance();

        if (node instanceof TestCaseReportNode) {
            measurement.setType(TEST_CASE);
        } else if (isArtefactInstrumented(artefactInstance)) {
            measurement.setType(TYPE_CUSTOM);
            measurement.addCustomField(ORIGIN, artefactInstance.getAttribute(AbstractOrganizableObject.NAME));
        }
        return measurement;
    }


    @Override
    public void afterReportNodeExecution(ExecutionContext executionContext, ReportNode node) {
        if (!generateMetrics(executionContext)) {
            return;
        }
        AbstractArtefact artefactInstance = node.getArtefactInstance();
        //Case report node is a keyword or an instrumented node
        if (node instanceof CallFunctionReportNode || node instanceof TestCaseReportNode || isArtefactInstrumented(artefactInstance)) {
            List<Measurement> measurements = new ArrayList<>();


            //For keyword call, get additional measures
            if (node instanceof CallFunctionReportNode) {
                CallFunctionReportNode functionReport = (CallFunctionReportNode) node;

                if (functionReport.getMeasures() != null) {
                    for (Measure measure : functionReport.getMeasures()) {
                        Measurement measurement = createMeasurement(executionContext, measure, functionReport);
                        measurements.add(measurement);
                    }
                }
            } else {
                measurements.add(transformToMeasurement(executionContext, node));
            }
            //Delegate to plugins implementation
            if (!measurements.isEmpty()) {
                processMeasurements(measurements);
            }
            //For keyword call, process output metrics
            if (node instanceof CallFunctionReportNode) {
                CallFunctionReportNode functionReport = (CallFunctionReportNode) node;
                List<MetricSample> outputMetrics = functionReport.getMetrics();
                if (outputMetrics != null && !outputMetrics.isEmpty()) {
                    List<ExecutionMetricSample> executionMetricSamples = outputMetrics.stream()
                        .map(m -> createExecutionMetricSample(executionContext, m, functionReport, null))
                        .collect(Collectors.toList());
                    processMetrics(executionMetricSamples);
                }
            }
        }
        if (node instanceof ThreadReportNode) {
            processThreadReportNode(executionContext, (ThreadReportNode) node, false);
        }
    }

    /**
     * Create a {@link Measurement} from keyword Measure, used both when processing Keyword's output and live measure
     * @param executionContext the execution context of the functionReport
     * @param measure the measure to be converted to Step Measurement
     * @param functionReport the function report of the function call producing the Measure
     * @return the created Measurement
     */
    private Measurement createMeasurement(ExecutionContext executionContext, Measure measure, CallFunctionReportNode functionReport) {
        Map<String, String> functionAttributes = functionReport.getFunctionAttributes();
        Measurement measurement = initMeasurement(executionContext);
        if (functionAttributes != null) {
            measurement.addCustomFields(functionAttributes);
            measurement.addCustomField(ORIGIN, functionAttributes.get(AbstractOrganizableObject.NAME));
        }
        measurement.setName(measure.getName());
        if (measure.getStatus() != null) {
            // Note: status should always be set for live measures, but is null unless explicitly set for "output measures".
            // The final value will always be set in enrichWithNodeAttributes (called below), even in case it was missing.
            measurement.setStatus(measure.getStatus().name());
        }
        measurement.setType(getMeasureTypeOrDefault(measure));
        measurement.setValue(measure.getDuration());
        measurement.setBegin(measure.getBegin());
        measurement.addCustomField(AGENT_URL, functionReport.getAgentUrl());
        enrichWithNodeAttributes(measurement, functionReport);
        enrichWithCustomData(measurement, measure.getData());
        enrichWithAdditionalAttributes(measurement, executionContext);
        return measurement;
    }

    /**
     * Create a {@link ExecutionMetricSample} from the keyword's {@link MetricSample}, used both when processing Keyword's output and live metric samples
     * @param executionContext the execution context of the functionReport
     * @param metric the Metric Sample to be converted to Step ExecutionMetricSample
     * @param functionReport the function report of the function call producing the samples, null for samples not produced by call keywords
     * @return the created ExecutionMetricSample
     */
    private ExecutionMetricSample createExecutionMetricSample(ExecutionContext executionContext, MetricSample metric,
                                                              CallFunctionReportNode functionReport, String metricType) {
        Plan plan = executionContext.getPlan();
        String planId = plan.getId().toString();
        String planName = Objects.requireNonNullElse(plan.getAttribute(AbstractOrganizableObject.NAME), "");
        String taskId = Objects.requireNonNullElse((String) executionContext.get(CTX_SCHEDULER_TASK_ID), "");
        String schedule = Objects.requireNonNullElse((String) executionContext.get(CTX_SCHEDULE_NAME), "");
        String execution = Objects.requireNonNullElse((String) executionContext.get(CTX_EXECUTION_DESCRIPTION), "");
        String execId = executionContext.getExecutionId();
        String rnId = (functionReport != null) ? functionReport.getId().toString() : null;
        String agentUrl = (functionReport != null) ? functionReport.getAgentUrl() : null;
        Map<String, String> functionAttributes = (functionReport != null) ? functionReport.getFunctionAttributes(): null;
        String origin = (functionAttributes != null) ? functionAttributes.get(AbstractOrganizableObject.NAME) : null;
        TreeMap<String, String> additionalAttributes = (TreeMap<String, String>) executionContext.get(CTX_ADDITIONAL_ATTRIBUTES);
        return new ExecutionMetricSample(metric, execId, rnId, planId, planName, taskId, schedule, execution,
            agentUrl, origin, additionalAttributes, metricType);
    }

    public void processMetrics(List<ExecutionMetricSample> metrics) {
        processMetrics(metrics, null);
    }

    public void processMetrics(List<ExecutionMetricSample> metrics, Map<String, String> optionalLabels) {
        for (MetricSamplesHandler metricSamplesHandler : SamplesExecutionPlugin.SAMPLES_HANDLERS) {
            try {
                metricSamplesHandler.processMetrics(metrics, optionalLabels);
            } catch (Exception e) {
                logger.error("Metrics could not be processed by " + metricSamplesHandler.getClass().getSimpleName(), e);
            }
        }
    }

    private static String getMeasureTypeOrDefault(Measure measure) {
        String type = null;
        Map<String, Object> data = measure.getData();
        if (data != null) {
            type = (String) measure.getData().get(MeasureTypes.ATTRIBUTE_TYPE);
        }
        if (type == null) {
            type = MeasureTypes.TYPE_CUSTOM;
        }
        return type;
    }

    protected Measurement initMeasurement(ExecutionContext executionContext) {
        Measurement measurement = new Measurement();
        Plan plan = executionContext.getPlan();
        measurement.setPlanId(plan.getId().toString());
        measurement.setPlan(Objects.requireNonNullElse(plan.getAttribute(AbstractOrganizableObject.NAME), ""));
        measurement.setExecution(Objects.requireNonNullElse((String) executionContext.get(CTX_EXECUTION_DESCRIPTION), ""));
        measurement.setTaskId(Objects.requireNonNullElse((String) executionContext.get(CTX_SCHEDULER_TASK_ID), ""));
        measurement.setSchedule(Objects.requireNonNullElse((String) executionContext.get(CTX_SCHEDULE_NAME), ""));
        return measurement;
    }

    public void processMeasurements(List<Measurement> measurements) {
        for (MetricSamplesHandler metricSamplesHandler : SamplesExecutionPlugin.SAMPLES_HANDLERS) {
            try {
                metricSamplesHandler.processMeasurements(measurements);
            } catch (Exception e) {
                logger.error("Measurement count not be processed by " + metricSamplesHandler.getClass().getSimpleName(), e);
            }
        }
    }

    private boolean isArtefactInstrumented(AbstractArtefact artefactInstance) {
        return artefactInstance != null && artefactInstance.getInstrumentNode().get();
    }

    private void enrichWithNodeAttributes(Measurement measurement, ReportNode node) {
        measurement.setExecId(node.getExecutionID());
        measurement.addCustomField(RN_ID, node.getId().toString());
        // If a measurement already has its own status (mandatory for live measures, optional for output measures),
        // keep it unconditionally, otherwise set the status from the report node.
        if (measurement.getStatus() == null) {
            measurement.setStatus(node.getStatus().name());
        }
    }

    private void enrichWithCustomData(Measurement measurement, Map<String, Object> data) {
        if (data != null) {
            data.forEach((key, val) -> {
                if ((key != null) && (val != null)) {
                    if ((val instanceof Long) || (val instanceof String)) {
                        measurement.addCustomField(key, val);
                    } else {
                        if ((val instanceof Number)) {
                            measurement.addCustomField(key, ((Integer) val).longValue());
                        } /*else {
                            // ignore improper types
                        }*/
                    }
                }
            });
        }
    }

    private void enrichWithAdditionalAttributes(Measurement measurement, ExecutionContext executionContext) {
        TreeMap<String, String> additionalAttributes = (TreeMap<String, String>) executionContext.get(CTX_ADDITIONAL_ATTRIBUTES);
        if (additionalAttributes != null) {
            measurement.addAdditionalAttributes(additionalAttributes);
        }
    }

    @Override
    public void afterExecutionEnd(ExecutionContext context) {
        if (generateMetrics(context)) {
            // 1. generate execution metrics
            createExecutionMetrics(context);
            // 2. notify handler
            for (MetricSamplesHandler metricSamplesHandler : SamplesExecutionPlugin.SAMPLES_HANDLERS) {
                metricSamplesHandler.afterExecutionEnd(context);
            }
            // 3. Cleanup
            MetricHeartbeatRegistry.getInstance().removeExecution(context.getExecutionId());
            // Clean up thread group counters for this execution.
            // Prometheus label cleanup (with the 70s scrape-window delay) is handled by PrometheusHandler.
            String prefix = context.getExecutionId() + "|";
            threadGroupCounts.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
        }
    }

    private void createExecutionMetrics(ExecutionContext context) {
        ExecutionAccessor executionAccessor = context.getExecutionAccessor();
        String executionId = context.getExecutionId();
        Execution execution = executionAccessor.get(executionId);
        ViewManager viewManager = context.require(ViewManager.class);
        ExecutionTypeManager executionTypeManager = context.require(ExecutionTypeManager.class);

        if (executionTypeManager.get(execution.getExecutionType()).generateExecutionMetrics()) {
            boolean executionPassed = execution.getResult() == ReportNodeStatus.PASSED;
            long startTime = execution.getStartTime();
            List<ExecutionMetricSample> samples = new ArrayList<>();
            MetricSample executionCount = new CounterMetric(EXECUTIONS_COUNT).increment(1, startTime).flush();
            samples.add(createExecutionMetricSample(context, executionCount, null, EXECUTIONS_COUNT));
            MetricSample failurePercentage = new GaugeMetric(FAILURE_PERCENTAGE).observe(executionPassed ? 0 : 100, startTime).flush();
            samples.add(createExecutionMetricSample(context, failurePercentage, null, FAILURE_PERCENTAGE));
            MetricSample failureCount = new CounterMetric(FAILURE_COUNT).increment(executionPassed ? 0 : 1, startTime).flush();
            samples.add(createExecutionMetricSample(context, failureCount, null, FAILURE_COUNT));

            long endTime = Objects.requireNonNullElse(execution.getEndTime(), System.currentTimeMillis());
            long duration = endTime - startTime;
            MetricSample executionDuration = new HistogramMetric(EXECUTIONS_DURATION,
                Map.of(EXECUTION_RESULT.getName(), execution.getResult().toString(), EXECUTION_BOOLEAN_RESULT.getName(), executionPassed ? "PASSED" : "FAILED"))
                .observe(duration, startTime).flush();
            samples.add(createExecutionMetricSample(context, executionDuration, null, EXECUTIONS_DURATION));

            ErrorDistribution errorDistribution = (ErrorDistribution) viewManager.queryView(ErrorDistributionView.ERROR_DISTRIBUTION_VIEW, executionId);
            errorDistribution.getCountByErrorCode().entrySet().forEach(entry -> {
                MetricSample failureCountByErrorCode = new GaugeMetric(FAILURES_COUNT_BY_ERROR_CODE,
                    Map.of(ERROR_CODE, entry.getKey()))
                    .observe(entry.getValue() > 0 ? 1 : 0, startTime).flush();
                samples.add(createExecutionMetricSample(context, failureCountByErrorCode, null, FAILURES_COUNT_BY_ERROR_CODE));
            });

            //Optional labels only used for now by the Prometheus handler
            Map<String, String> optionalLabelsMap = new TreeMap<>();
            optionalLabelsMap.put("executionUrl", String.format("%s/#/executions/%s", controllerUrl, executionId));
            optionalLabelsMap.put("startTime", String.valueOf(startTime));
            optionalLabelsMap.put("endtime", String.valueOf(endTime));
            ExecutiontTaskParameters executiontTaskParameters = execution.getExecutiontTaskParameters();
            optionalLabelsMap.put("cronExpression", (executiontTaskParameters != null) ? executiontTaskParameters.getCronExpression() : "");
            optionalLabelsMap.put("user", execution.getExecutionParameters().getUserID());
            Object automationPackageId = context.getPlan().getCustomField(AUTOMATION_PACKAGE_ID);
            optionalLabelsMap.put("apId", (automationPackageId != null) ? automationPackageId.toString() : "");
            AutomationPackageAccessor automationPackageAccessor = context.get(AutomationPackageAccessor.class);
            AutomationPackage automationPackage = (automationPackageAccessor != null && automationPackageId != null) ? automationPackageAccessor.get(automationPackageId.toString()) : null;
            optionalLabelsMap.put("apName", Optional.ofNullable(automationPackage).map(ap -> ap.getAttribute(AbstractOrganizableObject.NAME)).orElse(""));
            optionalLabelsMap.put("apVersionName", Optional.ofNullable(automationPackage).map(AutomationPackage::getVersionName).orElse(""));
            optionalLabelsMap.put("apLastModified", Optional.ofNullable(automationPackage).map(AbstractTrackedObject::getLastModificationDate).map(d -> String.valueOf(d.toInstant().toEpochMilli())).orElse(""));
            processMetrics(samples, optionalLabelsMap);
        }
    }
}
