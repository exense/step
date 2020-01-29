/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.core.execution;

import java.util.HashMap;

import org.bson.types.ObjectId;

import step.core.GlobalContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.RepositoryObjectReference;
import step.core.scheduler.ExecutionTaskAccessor;
import step.core.scheduler.ExecutiontTaskParameters;

public class ExecutionRunnableFactory {

	private GlobalContext globalContext;
	private ControllerExecutionContextBuilder executionContextBuilder;
	private ExecutionTaskAccessor taskAccessor;
	private PlanAccessor planAccessor;

	public ExecutionRunnableFactory(GlobalContext globalContext) {
		super();
		this.globalContext = globalContext;
		executionContextBuilder = new ControllerExecutionContextBuilder(globalContext);
		taskAccessor = globalContext.getScheduleAccessor();
		planAccessor = globalContext.getPlanAccessor();
	}

	public ExecutionRunnable newExecutionRunnable(Execution execution) {		
		ExecutionContext context = executionContextBuilder.createExecutionContext(execution.getId().toString(), execution.getExecutionParameters());
		ExecutionRunnable task = new ExecutionRunnable(globalContext.getRepositoryObjectManager(), globalContext.getExecutionAccessor(), context);
		return task;
	}

	public Execution createExecution(ExecutionParameters executionParameters, String taskID) {		
		Execution execution = new Execution();
		execution.setStartTime(System.currentTimeMillis());
		execution.setExecutionParameters(executionParameters);
		execution.setStatus(ExecutionStatus.INITIALIZING);
		execution.setAttributes(new HashMap<>());
		
		if(executionParameters.getAttributes() != null) {
			execution.getAttributes().putAll(executionParameters.getAttributes());
		}

		if(taskID != null) {
			ExecutiontTaskParameters executiontTaskParameters = taskAccessor.get(new ObjectId(taskID));
			if(executiontTaskParameters != null && executiontTaskParameters.getAttributes() != null)
				execution.getAttributes().putAll(executiontTaskParameters.getAttributes());
				execution.setExecutionTaskID(taskID);
		}

		Plan plan = null;
		RepositoryObjectReference repositoryObject = executionParameters.getRepositoryObject();
		if (repositoryObject != null &&
			repositoryObject.getRepositoryParameters().containsKey(RepositoryObjectReference.PLAN_ID)) {
			plan = planAccessor.get(new ObjectId(repositoryObject.getRepositoryParameters().get(RepositoryObjectReference.PLAN_ID)));
		}
		
		if(plan != null && plan.getAttributes() != null) {
			execution.getAttributes().putAll(plan.getAttributes());
		}
		
		if(executionParameters.getDescription()!=null) {
			execution.setDescription(executionParameters.getDescription());
		}

		globalContext.getExecutionAccessor().save(execution);		
		return execution;
	}

}
