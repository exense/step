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
package step.core.scheduler;

import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.model.ExecutionParameters;

public class ExecutiontTaskParameters extends AbstractOrganizableObject {
	
	public String name;
	
	public ExecutionParameters executionsParameters;
	
	public String cronExpression;
	
	public boolean active;

	public ExecutiontTaskParameters() {
		super();
	}

	public ExecutiontTaskParameters(
			ExecutionParameters executionsParameters, String cronExpr) {
		super();
		this.executionsParameters = executionsParameters;
		this.cronExpression = cronExpr;
		this.active = true;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ExecutionParameters getExecutionsParameters() {
		return executionsParameters;
	}

	public String getCronExpression() {
		return cronExpression;
	}

	public void setExecutionsParameters(ExecutionParameters executionsParameters) {
		this.executionsParameters = executionsParameters;
	}

	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	

}
