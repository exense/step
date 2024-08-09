package step.plugins.measurements;

import groovy.transform.Synchronized;
import io.prometheus.client.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.reports.CallFunctionReportNode;
import step.artefacts.reports.ThreadReportNode;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.model.Execution;
import step.core.plans.Plan;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.reports.Measure;
import step.core.scheduler.ExecutiontTaskParameters;
import step.engine.plugins.AbstractExecutionEnginePlugin;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static step.plugins.measurements.MeasurementControllerPlugin.ThreadgroupGaugeName;

@Plugin
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
	public static final String EXECUTION_DESCRIPTION = "execution";
	public static final String CTX_SCHEDULER_TASK_ID = "$schedulerTaskId";
	public static final String CTX_SCHEDULE_NAME = "$scheduleName";
	public static final String CTX_EXECUTION_DESCRIPTION = "$executionDescription";
	private static final Logger logger = LoggerFactory.getLogger(MeasurementPlugin.class);
	private static final List<MeasurementHandler> measurementHandlers = new ArrayList<>();
	public static final String CTX_GENERATE_EXECUTION_METRICS = "$generateExecutionMetrics";
	public static final String CTX_ADDITIONAL_ATTRIBUTES = "$additionalAttributes";

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
			executionContext.put(CTX_EXECUTION_DESCRIPTION, execution.getDescription());
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

	@Override
	public void afterReportNodeExecution(ExecutionContext executionContext, ReportNode node) {
		if (!generateMetrics(executionContext)) {
			return;
		}
		AbstractArtefact artefactInstance = node.getArtefactInstance();
		//Case report node is a keyword or an instrumented node
		if (node instanceof CallFunctionReportNode || isArtefactInstrumented(artefactInstance)) {
			List<Measurement> measurements = new ArrayList<>();


			//For keyword call, get additional measures
			if (node instanceof CallFunctionReportNode) {
				CallFunctionReportNode functionReport = (CallFunctionReportNode) node;

				if (functionReport.getMeasures() != null) {
					for (Measure measure : functionReport.getMeasures()) {
						Map<String, String> functionAttributes = functionReport.getFunctionAttributes();
						Measurement measurement = initMeasurement(executionContext);
						measurement.addCustomFields(functionAttributes);
						measurement.setName(measure.getName());
						measurement.setType(measure.getData().get(TYPE).toString());
						measurement.addCustomField(ORIGIN, functionAttributes.get(AbstractOrganizableObject.NAME));
						measurement.setValue(measure.getDuration());
						measurement.setBegin(measure.getBegin());
						measurement.addCustomField(AGENT_URL, functionReport.getAgentUrl());
						enrichWithNodeAttributes(measurement, node);
						enrichWithCustomData(measurement, measure.getData());
						enrichWithAdditionalAttributes(measurement, executionContext);
						measurements.add(measurement);
					}
				}
			}
			//Add measure of instrumented nodes
			if (isArtefactInstrumented(artefactInstance)) {
				Measurement measurement = initMeasurement(executionContext);
				measurement.setName(node.getName());
				measurement.addCustomField(ORIGIN, artefactInstance.getAttribute(AbstractOrganizableObject.NAME));
				measurement.setValue((long) node.getDuration());
				measurement.setBegin(node.getExecutionTime());
				measurement.setType(TYPE_CUSTOM);
				enrichWithNodeAttributes(measurement, node);
				enrichWithAdditionalAttributes(measurement, executionContext);
				measurements.add(measurement);
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
		measurement.setStatus(node.getStatus().toString());
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
