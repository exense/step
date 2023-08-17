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

import org.bson.types.ObjectId;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.model.ExecutionParameters;
import step.core.objectenricher.EnricheableObject;

public class ExecutiontTaskParameters extends AbstractOrganizableObject implements EnricheableObject {
	
	// TODO remove this field and create a migration task to also remove it from the DB
	public String name;
	
	public ExecutionParameters executionsParameters;

	private ObjectId assertionPlan;

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

	/**
	 * @deprecated This field isn't used anymore. The task name is now persisted as
	 *             attribute.Please use getAttribute("name") instead
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @deprecated This field isn't used anymore. The task name is now persisted as
	 *             attribute.Please use setAttribute("name","...") instead
	 * @param name
	 */
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

	public ObjectId getAssertionPlan() {
		return assertionPlan;
	}

	public void setAssertionPlan(ObjectId assertionPlan) {
		this.assertionPlan = assertionPlan;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExecutiontTaskParameters other = (ExecutiontTaskParameters) obj;
		if (getId() == null) {
			if (other.getId() != null)
				return false;
		} else if (!getId().equals(other.getId()))
			return false;
		return true;
	}
	
	

}
