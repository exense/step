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
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.reports.Measure;
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
	public static final String SCHEDULER_TASK_ID = "$schedulerTaskId";

	private static final Logger logger = LoggerFactory.getLogger(MeasurementPlugin.class);

	private static List<MeasurementHandler> measurementHandlers = new ArrayList<>();
	private Map<String, Set<String[]>> labelsByExec = new ConcurrentHashMap<>();

	GaugeCollectorRegistry gaugeCollectorRegistry;

	public MeasurementPlugin(GaugeCollectorRegistry gaugeCollectorRegistry) {
		this.gaugeCollectorRegistry = gaugeCollectorRegistry;
	}

	public static synchronized void registerMeasurementHandlers(MeasurementHandler handler) {
		measurementHandlers.add(handler);
	}
	@Override
	public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
		Execution execution = executionContext.getExecutionAccessor().get(executionContext.getExecutionId());
		String schedulerTaskId = execution.getExecutionTaskID();
		if (schedulerTaskId != null) {
			// cache the scheduler task id in the context to avoid retrieval of the
			// execution after each report nod execution
			executionContext.put(SCHEDULER_TASK_ID, schedulerTaskId);
		}

		if (!labelsByExec.containsKey(executionContext.getExecutionId())) {
			labelsByExec.put(executionContext.getExecutionId(), new HashSet<>());
		}

		for (MeasurementHandler measurementHandler : MeasurementPlugin.measurementHandlers) {
			measurementHandler.initializeExecutionContext(executionEngineContext,executionContext);
		}
	}

	@Override
	public void afterExecutionEnd(ExecutionContext context) {
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

	@Override
	public void beforeReportNodeExecution(ExecutionContext context, ReportNode node) {
		if (node instanceof ThreadReportNode) {
			processThreadReportNode(context, (ThreadReportNode) node, true);
		}
	}

	@Synchronized
	private GaugeCollector getOrInitThreadGauge(ExecutionContext context) {
		GaugeCollector gaugeCollector = gaugeCollectorRegistry.getGaugeCollector(ThreadgroupGaugeName);
		if (gaugeCollector == null) {
			List<String> labelsThreadGroup = new ArrayList<>(Arrays.asList(ATTRIBUTE_EXECUTION_ID, NAME, PLAN_ID, TASK_ID));
			Set<String> additionalAttributeKeys = context.getObjectEnricher().getAdditionalAttributes().keySet();
			labelsThreadGroup.addAll(additionalAttributeKeys);

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
		GaugeCollector gaugeCollector = getOrInitThreadGauge(context);
		String schedulerTaskId = (String) context.get(SCHEDULER_TASK_ID);
		schedulerTaskId = (schedulerTaskId!=null) ? schedulerTaskId : "";
		String planId = context.getPlan().getId().toString();
		List<String> labels = new ArrayList<>(Arrays.asList(node.getExecutionID(), node.getThreadGroupName(), planId, schedulerTaskId));
		labels.addAll(context.getObjectEnricher().getAdditionalAttributes().values());
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
		AbstractArtefact artefactInstance = node.getArtefactInstance();
		//Case report node is a keyword or an instrumented node
		if (node instanceof CallFunctionReportNode || isArtefactInstrumented(artefactInstance)) {
			List<Measurement> measurements = new ArrayList<>();

			String schedulerTaskId = (String) executionContext.get(SCHEDULER_TASK_ID);
			String planId = executionContext.getPlan().getId().toString();
			//For keyword call, get additional measures
			if (node instanceof CallFunctionReportNode) {
				CallFunctionReportNode functionReport = (CallFunctionReportNode) node;

				if (functionReport.getMeasures() != null) {
					for (Measure measure : functionReport.getMeasures()) {
						Map<String, String> functionAttributes = functionReport.getFunctionAttributes();
						Measurement measurement = new Measurement();
						measurement.addCustomFields(functionAttributes);
						measurement.setName(measure.getName());
						measurement.setType(measure.getData().get(TYPE).toString());
						measurement.addCustomField(ORIGIN, functionAttributes.get(AbstractOrganizableObject.NAME));
						measurement.setValue(measure.getDuration());
						measurement.setBegin(measure.getBegin());
						measurement.addCustomField(AGENT_URL, functionReport.getAgentUrl());
						enrichWithNodeAttributes(measurement, node, schedulerTaskId, planId);
						enrichWithCustomData(measurement, measure.getData());
						measurements.add(measurement);
					}
				}
			}
			//Add measure of instrumented nodes
			if (isArtefactInstrumented(artefactInstance)) {
				Measurement measurement = new Measurement();
				measurement.setName(node.getName());
				measurement.addCustomField(ORIGIN, artefactInstance.getAttribute(AbstractOrganizableObject.NAME));
				measurement.setValue((long) node.getDuration());
				measurement.setBegin(node.getExecutionTime());
				measurement.setType(TYPE_CUSTOM);
				enrichWithNodeAttributes(measurement, node, schedulerTaskId, planId);

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

	protected void processMeasurements(List<Measurement> measurements) {
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

	private void enrichWithNodeAttributes(Measurement measurement, ReportNode node, String schedulerTaskId, String planId) {
		measurement.setExecId(node.getExecutionID());

		measurement.addCustomField(RN_ID, node.getId().toString());
		measurement.setStatus(node.getStatus().toString());
		measurement.setPlanId(planId);
		if (schedulerTaskId != null) {
			measurement.setTaskId(schedulerTaskId);
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




}
