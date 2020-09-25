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
package step.core.execution.table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.execution.model.Execution;
import step.core.execution.type.ExecutionType;
import step.core.execution.type.ExecutionTypeManager;

public class ExecutionSummaryProvider {
				
	private ExecutionTypeManager executionTypeManager;
	
	private static final Logger logger = LoggerFactory.getLogger(ExecutionSummaryProvider.class);

	public ExecutionSummaryProvider(GlobalContext context) {
		super();
		executionTypeManager = context.get(ExecutionTypeManager.class);
	}

	public Object format(Execution execution) {
		String executionTypeName = execution.getExecutionType();
		ExecutionType executionType = executionTypeManager.get(executionTypeName);
		try {
			if(executionType!=null) {
				Object result = executionType.getExecutionSummary(execution.getId().toString());
				return result;				
			} else {
				logger.warn("Execution type "+executionTypeName+ " not available");
			}
		} catch (Exception e) {
			logger.error("Error while getting execution summary for execution "+execution.getId().toString(), e);
		}			
		return null;
	}
}
