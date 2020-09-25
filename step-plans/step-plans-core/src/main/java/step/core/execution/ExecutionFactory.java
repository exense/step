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
package step.core.execution;

import java.util.HashMap;

import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;

public class ExecutionFactory {
	
	public Execution createExecution(ExecutionParameters executionParameters, String taskID) {		
		Execution execution = new Execution();
		execution.setStartTime(System.currentTimeMillis());
		execution.setExecutionParameters(executionParameters);
		execution.setStatus(ExecutionStatus.INITIALIZING);
		execution.setAttributes(new HashMap<>());
		
		if(executionParameters.getAttributes() != null) {
			execution.getAttributes().putAll(executionParameters.getAttributes());
		}

//		if(objectEnricher != null) {
//			objectEnricher.accept(execution);
//		}
		
		execution.setExecutionTaskID(taskID);
		
		if(executionParameters.getDescription()!=null) {
			execution.setDescription(executionParameters.getDescription());
		}

		return execution;
	}
}
