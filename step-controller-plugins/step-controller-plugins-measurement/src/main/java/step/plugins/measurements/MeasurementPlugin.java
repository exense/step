package step.plugins.measurements;

import groovy.transform.Synchronized;
import io.prometheus.client.Collector;
import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.reports.CallFunctionReportNode;
import step.artefacts.reports.TestCaseReportNode;
import step.artefacts.reports.ThreadReportNode;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.model.Execution;
import step.core.plans.Plan;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.reports.Measure;
import step.core.scheduler.ExecutiontTaskParameters;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.functions.Function;
import step.functions.handler.MeasureTypes;
import step.functions.io.Output;
import step.livereporting.LiveReportingPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static step.plugins.measurements.MeasurementControllerPlugin.ThreadgroupGaugeName;

@Plugin(dependencies = LiveReportingPlugin.class)
@IgnoreDuringAutoDiscovery
public class MeasurementPlugin extends AbstractExecutionEnginePlugin {

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
	public static final String PLAN_ID = "planId";
	public static final String PLAN = "plan";
	public static final String SCHEDULE	= "schedule";
	public static final String TEST_CASE = "testcase";
	public static final String EXECUTION_DESCRIPTION = "execution";
	public static final String CTX_SCHEDULER_TASK_ID = "$schedulerTaskId";
	public static final String CTX_SCHEDULE_NAME = "$scheduleName";
	public static final String CTX_EXECUTION_DESCRIPTION = "$executionDescription";
	private static final Logger logger = LoggerFactory.getLogger(MeasurementPlugin.class);
	private static final List<MeasurementHandler> measurementHandlers = new ArrayList<>();
	public static final String CTX_GENERATE_EXECUTION_METRICS = "$generateExecutionMetrics";
	public static final String CTX_ADDITIONAL_ATTRIBUTES = "$additionalAttributes";

	// These are used by the MeasurementControllerPlugin to "reconstruct" measures from measurements, and indicate the
	// "internal" fields which should NOT be added to the measure data field. Keep this in sync with the fields defined above.
	static final Set<String> MEASURE_NOT_DATA_KEYS = Set.of("_id", "project", "projectName", ATTRIBUTE_EXECUTION_ID, RN_ID,
			ORIGIN, RN_STATUS, STATUS, PLAN_ID, PLAN, AGENT_URL, TASK_ID, SCHEDULE, TEST_CASE, EXECUTION_DESCRIPTION);
	// Same use, but for defining which fields SHOULD be directly copied to the top-level fields of a measure.
	static final Set<String> MEASURE_FIELDS = Set.of(NAME, BEGIN, VALUE);

	private final Map<String, Set<String[]>> labelsByExec = new ConcurrentHashMap<>();
	private final GaugeCollectorRegistry gaugeCollectorRegistry;
	private GaugeCollector gaugeCollector;

	public MeasurementPlugin(GaugeCollectorRegistry gaugeCollectorRegistry) {
		this.gaugeCollectorRegistry = gaugeCollectorRegistry;
	}

	public static synchronized void registerMeasurementHandlers(MeasurementHandler handler) {
		measurementHandlers.add(handler);
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
			if(description != null) {
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
			TreeMap<String, String> additionalAttributes = Objects.requireNonNullElse(executionContext .getObjectEnricher().getAdditionalAttributes(), new TreeMap<>());
			getOrInitThreadGauge(additionalAttributes);
			executionContext.put(CTX_ADDITIONAL_ATTRIBUTES, additionalAttributes);

			if (!labelsByExec.containsKey(executionContext.getExecutionId())) {
				labelsByExec.put(executionContext.getExecutionId(), new HashSet<>());
			}

			for (MeasurementHandler measurementHandler : MeasurementPlugin.measurementHandlers) {
				measurementHandler.initializeExecutionContext(executionEngineContext, executionContext);
			}
		}
	}

	private boolean generateMetrics(ExecutionContext executionContext) {
		return (boolean) executionContext.require(CTX_GENERATE_EXECUTION_METRICS);
	}

	@Override
	public void afterExecutionEnd(ExecutionContext context) {
		if (generateMetrics(context)) {
			for (MeasurementHandler measurementHandler : MeasurementPlugin.measurementHandlers) {
				measurementHandler.afterExecutionEnd(context);
			}
			//Clean up gauge metrics for execution id
			GaugeCollector gaugeCollector = gaugeCollectorRegistry.getGaugeCollector(ThreadgroupGaugeName);
			ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
			Runnable task = new Runnable() {
				public void run() {
					for (String[] labels : labelsByExec.remove(context.getExecutionId())) {
						gaugeCollector.getGauge().remove(labels);
					}
				}
			};
			int delay = 70;
			scheduler.schedule(task, delay, TimeUnit.SECONDS);
			scheduler.shutdown();
		}
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
	}

	@Override
	public void afterFunctionExecution(ExecutionContext context, ReportNode node, Function function, Output<JsonObject> output) {

	}

	@Synchronized
	private GaugeCollector getOrInitThreadGauge(Map<String, String> additionalAttributes) {
		gaugeCollector = gaugeCollectorRegistry.getGaugeCollector(ThreadgroupGaugeName);
		if (gaugeCollector == null) {
			List<String> labelsThreadGroup = new ArrayList<>(Arrays.asList(ATTRIBUTE_EXECUTION_ID, "execution", NAME, PLAN_ID,
					"plan", "scheduleId", "schedule"));
			labelsThreadGroup.addAll(additionalAttributes.keySet());

			gaugeCollector = new GaugeCollector(ThreadgroupGaugeName,
					"step thread group active threads count", labelsThreadGroup.toArray(String[]::new)) {
				@Override
				public List<Collector.MetricFamilySamples> collect() {
					return getGauge().collect();
				}
			};
			//Register thread group gauge metrics
			gaugeCollectorRegistry.registerCollector(ThreadgroupGaugeName, gaugeCollector);
		}
		return gaugeCollector;
	}

	private void processThreadReportNode(ExecutionContext context, ThreadReportNode node, boolean inc) {
		String scheduleId = Objects.requireNonNullElse((String) context.get(CTX_SCHEDULER_TASK_ID), "");
		String schedule = Objects.requireNonNullElse((String) context.get(CTX_SCHEDULE_NAME), "");
		String planId = context.getPlan().getId().toString();
		String plan =  Objects.requireNonNullElse(context.getPlan().getAttribute(AbstractOrganizableObject.NAME), "");
		String executionID = context.getExecutionId();
		String execution = Objects.requireNonNullElse((String) context.get(CTX_EXECUTION_DESCRIPTION), "");
		List<String> labels = new ArrayList<>(Arrays.asList(executionID, execution, node.getThreadGroupName(), planId, plan, scheduleId, schedule));
		Map<String, String> additionalAttributes = (Map<String, String>) context.get(CTX_ADDITIONAL_ATTRIBUTES);
		if (additionalAttributes != null) {
			labels.addAll((additionalAttributes).values());
		}
		String[] labelsArray = labels.toArray(String[]::new);
		if (inc) {
			gaugeCollector.getGauge().labels(labelsArray).inc();
		} else {
			gaugeCollector.getGauge().labels(labelsArray).dec();
		}
		labelsByExec.get(context.getExecutionId()).add(labelsArray);
		List<Measurement> measurements = gaugeCollector.collectAsMeasurements();
		for (MeasurementHandler measurementHandler : MeasurementPlugin.measurementHandlers) {
			measurementHandler.processGauges(measurements);
		}
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
		}
		if (node instanceof ThreadReportNode) {
			processThreadReportNode(executionContext, (ThreadReportNode) node, false);
		}
	}

	private Measurement createMeasurement(ExecutionContext executionContext, Measure measure, CallFunctionReportNode functionReport) {
		Map<String, String> functionAttributes = functionReport.getFunctionAttributes();
		Measurement measurement = initMeasurement(executionContext);
		measurement.addCustomFields(functionAttributes);
		measurement.setName(measure.getName());
		if (measure.getStatus() != null) {
			// Note: status SHOULD always be non-null, but better safe than sorry.
			// The final value will always be set in enrichWithNodeAttributes (called below), even in case it was missing.
			measurement.setStatus(measure.getStatus().name());
		}
		measurement.setType(getMeasureTypeOrDefault(measure));
		measurement.addCustomField(ORIGIN, functionAttributes.get(AbstractOrganizableObject.NAME));
		measurement.setValue(measure.getDuration());
		measurement.setBegin(measure.getBegin());
		measurement.addCustomField(AGENT_URL, functionReport.getAgentUrl());
		enrichWithNodeAttributes(measurement, functionReport);
		enrichWithCustomData(measurement, measure.getData());
		enrichWithAdditionalAttributes(measurement, executionContext);
		return measurement;
	}

	private static String getMeasureTypeOrDefault(Measure measure) {
		String type = null;
		Map<String, Object> data = measure.getData();
		if (data != null) {
			type = (String) measure.getData().get(MeasureTypes.ATTRIBUTE_TYPE);
		}
		if(type == null) {
			type = MeasureTypes.TYPE_CUSTOM;
		}
		return type;
	}

	protected Measurement initMeasurement(ExecutionContext executionContext) {
		Measurement measurement = new Measurement();
		Plan plan = executionContext.getPlan();
		measurement.setPlanId(plan.getId().toString());
		measurement.setPlan(Objects.requireNonNullElse(plan.getAttribute(AbstractOrganizableObject.NAME),""));
		measurement.setExecution(Objects.requireNonNullElse((String) executionContext.get(CTX_EXECUTION_DESCRIPTION),""));
		measurement.setTaskId(Objects.requireNonNullElse((String) executionContext.get(CTX_SCHEDULER_TASK_ID),""));
		measurement.setSchedule(Objects.requireNonNullElse((String) executionContext.get(CTX_SCHEDULE_NAME),""));
		return measurement;
	}

	public void processMeasurements(List<Measurement> measurements) {
		for (MeasurementHandler measurementHandler : MeasurementPlugin.measurementHandlers) {
			try {
				measurementHandler.processMeasurements(measurements);
			} catch (Exception e) {
				logger.error("Measurement count not be processed by " + measurementHandler.getClass().getSimpleName(),e);
			}
		}
	}

	private boolean isArtefactInstrumented(AbstractArtefact artefactInstance) {
		return artefactInstance != null && artefactInstance.getInstrumentNode().get();
	}

	private void enrichWithNodeAttributes(Measurement measurement, ReportNode node) {
		measurement.setExecId(node.getExecutionID());
		measurement.addCustomField(RN_ID, node.getId().toString());
		ReportNodeStatus nodeStatus = node.getStatus();
		if(nodeStatus == ReportNodeStatus.RUNNING) {
			// For live measures, the node status is still RUNNING, so we rely on the measure status
			if (measurement.getStatus() == null) {
				// this should never be the case, as all measures should now have a status (PASSED by default), but better safe than sorry
				measurement.setStatus(nodeStatus.name());
			}
		} else {
			// "old-style" measures (attached to output) take the node status, EXCEPT if the measure itself indicates a non-default status, i.e. a failure
			String mStatus = measurement.getStatus();
			if (mStatus == null || mStatus.equals(Measure.Status.PASSED.name())) {
				measurement.setStatus(nodeStatus.name());
			}
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


}
