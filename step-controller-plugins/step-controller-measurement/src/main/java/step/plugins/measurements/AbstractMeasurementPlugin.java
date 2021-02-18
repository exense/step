package step.plugins.measurements;

import step.artefacts.reports.CallFunctionReportNode;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.model.Execution;
import step.core.reports.Measure;
import step.engine.plugins.AbstractExecutionEnginePlugin;

import java.util.*;

public abstract class AbstractMeasurementPlugin extends AbstractExecutionEnginePlugin {

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

	protected static final String SCHEDULER_TASK_ID = "$schedulerTaskId";

	@Override
	public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
		Execution execution = executionContext.getExecutionAccessor().get(executionContext.getExecutionId());
		String schedulerTaskId = execution.getExecutionTaskID();
		if (schedulerTaskId != null) {
			// cache the scheduler task id in the context to avoid retrieval of the
			// execution after each report nod execution
			executionContext.put(SCHEDULER_TASK_ID, schedulerTaskId);
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
			if (measurements.size() > 0) {
				processMeasurements(measurements, executionContext);
			}


		}
	}

	protected abstract void processMeasurements(List<Measurement> measurements, ExecutionContext executionContext);

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
