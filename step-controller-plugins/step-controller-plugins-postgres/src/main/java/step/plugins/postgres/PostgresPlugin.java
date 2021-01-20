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
package step.plugins.postgres;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class PostgresPlugin extends AbstractExecutionEnginePlugin {

	private static final String ATTRIBUTE_EXECUTION_ID  = "eId";
	private static final String TYPE_CUSTOM = "custom";
	private static final String TYPE_KW = "keyword";
	private static final String TYPE = "type";
	private static final String NAME = "name";
	private static final String BEGIN = "begin_t";
	private static final String VALUE = "value";
	private static final String RN_ID = "rnId";
	private static final String STATUS = "status";
	private static final String AGENT_URL = "agentUrl";
	private static final String ORIGIN = "origin";
	private static final String TASK_ID = "taskId";
	private static final String PLAN_ID = "planId";
	
	private static final String SCHEDULER_TASK_ID = "$schedulerTaskId";

	private static final Logger logger = LoggerFactory.getLogger(PostgresPlugin.class);

	private static String SQLinsert = "INSERT INTO measurements("+BEGIN+",info,"+ ATTRIBUTE_EXECUTION_ID +"," +
		STATUS +"," + PLAN_ID + ","+ TASK_ID+ "," + NAME + "," + TYPE + "," + VALUE + ") VALUES(?,?::jsonb,?,?,?,?,?,?,?)";

	public PostgresPlugin() {
		super();
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
		if (node instanceof CallFunctionReportNode || isArtefactInstrumented(artefactInstance)) {
			try (Connection postgresCon = DriverManager.getConnection(PostgresControllerPlugin.ConnectionPoolName);
					PreparedStatement preparedStatement = postgresCon.prepareStatement(SQLinsert, Statement.RETURN_GENERATED_KEYS)) {

				postgresCon.setAutoCommit(false);
				List<Map<String, Object>> measurements = new ArrayList<>();

				String schedulerTaskId = (String) executionContext.get(SCHEDULER_TASK_ID);
				String planId = executionContext.getPlan().getId().toString();

				if (node instanceof CallFunctionReportNode) {
					CallFunctionReportNode stepReport = (CallFunctionReportNode) node;

					if (stepReport.getMeasures() != null) {
						for (Measure measure : stepReport.getMeasures()) {
							try {
								//Create the json payload for the 'info' column
								Map<String, Object> info = new HashMap<>();
								Map<String, String> functionAttributes = stepReport.getFunctionAttributes();
								info.putAll(functionAttributes);
								info.put(ORIGIN, functionAttributes.get(AbstractOrganizableObject.NAME));
								info.put(AGENT_URL, stepReport.getAgentUrl());
								info.put(RN_ID, node.getId().toString());
								if (measure.getData() != null) {
									measure.getData().forEach((key, val) -> {
										if ((key != null) && (val != null)) {
											if ((val instanceof Long) || (val instanceof String)) {
												info.put(key, val);
											} else {
												if ((val instanceof Number)) {
													info.put(key, ((Integer) val).longValue());
												} else {
													// ignore improper types
												}
											}
										}
									});
								}

								addPrpStmtToBatch(preparedStatement, measure.getBegin(), info, stepReport.getExecutionID(),
										node.getStatus().toString(), planId, (schedulerTaskId != null) ? schedulerTaskId : "",
										measure.getName(), TYPE_KW, measure.getDuration());

							} catch (SQLException e) {
								logger.error("Error while setting values of prepared statement", e);
							} catch (JsonProcessingException e) {
								logger.error("Error while transforming map to json payload", e);
							}
						}
					}

				}
				if (isArtefactInstrumented(artefactInstance)) {
					try {
						Map<String, Object> info = new HashMap<>();
						info.put(ORIGIN, artefactInstance.getAttribute(AbstractOrganizableObject.NAME));
						info.put(RN_ID, node.getId().toString());

						addPrpStmtToBatch(preparedStatement, node.getExecutionTime(), info, node.getExecutionID(),
								node.getStatus().toString(), planId, (schedulerTaskId != null) ? schedulerTaskId : "",
								node.getName(), TYPE_CUSTOM, (long) node.getDuration());

					} catch (SQLException e) {
						logger.error("Error while setting values of prepared statement", e);
					} catch (JsonProcessingException e) {
						logger.error("Error while transforming map to json payload", e);
					}

				}

				int[] numUpdates = preparedStatement.executeBatch();
				for (int i = 0; i < numUpdates.length; i++) {
					if (numUpdates[i] < 0)
						logger.error("Batch updates failed for statement #" + i + ", " + preparedStatement.toString());
				}
				postgresCon.commit();
			} catch (SQLException e) {
				logger.error("Error while persisting measurements to postgress", e );
			}
		}

	}

	private void addPrpStmtToBatch(PreparedStatement preparedStatement, long executionTime, Map<String, Object> info,
								   String executionID, String status, String planId, String taskId, String name,
								   String type, long duration) throws SQLException, JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		preparedStatement.clearParameters();
		//begin_t,info,eId,status,planId,taskId,name,type,value
		preparedStatement.setTimestamp(1,new java.sql.Timestamp(executionTime));
		preparedStatement.setObject(2, objectMapper.writeValueAsString(info));
		preparedStatement.setString(3, executionID);
		preparedStatement.setString(4, status);
		preparedStatement.setString(5, planId);
		preparedStatement.setString(6, taskId);
		preparedStatement.setString(7, name);
		preparedStatement.setString(8, type);
		preparedStatement.setLong(9, duration);
		//execute statements in batch with autocommit = false
		preparedStatement.addBatch();
	}


	private boolean isArtefactInstrumented(AbstractArtefact artefactInstance) {
		return artefactInstance != null && artefactInstance.getInstrumentNode().get();
	}


}
