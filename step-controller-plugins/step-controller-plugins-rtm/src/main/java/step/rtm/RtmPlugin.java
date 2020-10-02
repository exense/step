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
		if(node instanceof CallFunctionReportNode) {
			CallFunctionReportNode stepReport = (CallFunctionReportNode) node;

			Map<String, String> functionAttributes = stepReport.getFunctionAttributes();

			String schedulerTaskId = (String) executionContext.get(SCHEDULER_TASK_ID);
			String planId = executionContext.getPlan().getId().toString();
			
			List<Object> measurements = new ArrayList<>();

			Map<String, Object> measurement;

			if(stepReport.getMeasures()!=null) {
				for(Measure measure:stepReport.getMeasures()) {
					measurement = new HashMap<>();

					measurement.putAll(functionAttributes);

					measurement.put(RtmControllerPlugin.ATTRIBUTE_EXECUTION_ID, stepReport.getExecutionID());
					measurement.put("name", measure.getName());
					measurement.put("origin", stepReport.getFunctionAttributes().get(AbstractOrganizableObject.NAME));
					measurement.put("value", measure.getDuration());
					measurement.put("begin", measure.getBegin());
					measurement.put("rnId", stepReport.getId().toString());
					measurement.put("rnStatus", stepReport.getStatus().toString());
					measurement.put("agentUrl", stepReport.getAgentUrl());
					measurement.put("planId", planId);
					if(schedulerTaskId != null) {
						measurement.put("taskId", schedulerTaskId);
					}
					

					if(measure.getData() != null){
						for(Map.Entry<String,Object> entry : measure.getData().entrySet()){
							String key = entry.getKey();
							Object val = entry.getValue();
							if((key != null) && (val != null)){
								if(	(val instanceof Long) || (val instanceof String)){
									measurement.put(key, val);
								}else{
									if(	(val instanceof Number)){
										measurement.put(key, ((Integer) val).longValue());
									}else{
										// ignore improper types
									}
								}
							}
						}
					}

					measurements.add(measurement);
				}
			}

			accessor.saveManyMeasurements(measurements);

			if (logger.isTraceEnabled()) {
				logMeasurements(measurements);
			}
		}
	}

	public static void logMeasurements(List<Object> measurements) {
		for (Object o: measurements) {
			logger.trace("RTM measure:" + o.toString());
		}
	}
}
