/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.rtm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rtm.commons.MeasurementAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.artefacts.reports.CallFunctionReportNode;
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

@Plugin
@IgnoreDuringAutoDiscovery
public class RtmPlugin extends AbstractExecutionEnginePlugin {

	private static final String TYPE_CUSTOM = "custom";
	private static final String TYPE = "type";
	private static final String NAME = "name";
	private static final String BEGIN = "begin";
	private static final String VALUE = "value";
	private static final String RN_ID = "rnId";
	private static final String RN_STATUS = "rnStatus";
	private static final String AGENT_URL = "agentUrl";
	private static final String ORIGIN = "origin";
	private static final String TASK_ID = "taskId";
	private static final String PLAN_ID = "planId";
	
	private static final String SCHEDULER_TASK_ID = "$schedulerTaskId";

	private static final Logger logger = LoggerFactory.getLogger(RtmPlugin.class);

	private final MeasurementAccessor accessor;

	public RtmPlugin(MeasurementAccessor accessor) {
		super();
		this.accessor = accessor;
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
	}

	@Override
	public void afterReportNodeExecution(ExecutionContext executionContext, ReportNode node) {		
		AbstractArtefact artefactInstance = node.getArtefactInstance();
		if(node instanceof CallFunctionReportNode || isArtefactInstrumented(artefactInstance)) {
			List<Object> measurements = new ArrayList<>();

			String schedulerTaskId = (String) executionContext.get(SCHEDULER_TASK_ID);
			String planId = executionContext.getPlan().getId().toString();

			if(node instanceof CallFunctionReportNode) {
				CallFunctionReportNode stepReport = (CallFunctionReportNode) node;
				
				if(stepReport.getMeasures()!=null) {
					for(Measure measure:stepReport.getMeasures()) {
						Map<String, String> functionAttributes = stepReport.getFunctionAttributes();
						Map<String, Object> measurement = new HashMap<>();
						measurement.putAll(functionAttributes);
						measurement.put(RtmControllerPlugin.ATTRIBUTE_EXECUTION_ID, stepReport.getExecutionID());
						measurement.put(NAME, measure.getName());
						measurement.put(ORIGIN, functionAttributes.get(AbstractOrganizableObject.NAME));
						measurement.put(VALUE, measure.getDuration());
						measurement.put(BEGIN, measure.getBegin());
						measurement.put(AGENT_URL, stepReport.getAgentUrl());
						enrichWithNodeAttributes(measurement, node, schedulerTaskId, planId);
						
						if (measure.getData() != null) {
							measure.getData().forEach((key, val) -> {
								if ((key != null) && (val != null)) {
									if ((val instanceof Long) || (val instanceof String)) {
										measurement.put(key, val);
									} else {
										if ((val instanceof Number)) {
											measurement.put(key, ((Integer) val).longValue());
										} else {
											// ignore improper types
										}
									}
								}
							});
						}
						
						measurements.add(measurement);
					}
				}
				
			}
			if (isArtefactInstrumented(artefactInstance)) {
				Map<String, Object> measurement = new HashMap<>();

				measurement.put(RtmControllerPlugin.ATTRIBUTE_EXECUTION_ID, node.getExecutionID());
				measurement.put(NAME, node.getName());
				measurement.put(ORIGIN, artefactInstance.getAttribute(AbstractOrganizableObject.NAME));
				measurement.put(VALUE, (long) node.getDuration());
				measurement.put(BEGIN, node.getExecutionTime());
				measurement.put(TYPE, TYPE_CUSTOM);
				enrichWithNodeAttributes(measurement, node, schedulerTaskId, planId);

				measurements.add(measurement);
			}

			if (measurements.size()>0) {
				accessor.saveManyMeasurements(measurements);
			}

			if (logger.isTraceEnabled()) {
				logMeasurements(measurements);
			}
		}
	}

	private boolean isArtefactInstrumented(AbstractArtefact artefactInstance) {
		return artefactInstance != null && artefactInstance.getInstrumentNode().get();
	}

	private void enrichWithNodeAttributes(Map<String, Object> measurement, ReportNode node, String schedulerTaskId, String planId) {
		measurement.put(RN_ID, node.getId().toString());
		measurement.put(RN_STATUS, node.getStatus().toString());
		measurement.put(PLAN_ID, planId);
		if(schedulerTaskId != null) {
			measurement.put(TASK_ID, schedulerTaskId);
		}
	}

	public static void logMeasurements(List<Object> measurements) {
		for (Object o: measurements) {
			logger.trace("RTM measure:" + o.toString());
		}
	}
}
