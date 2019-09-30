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
package step.plugins.parametermanager;

import step.commons.activation.ActivableObject;
import step.commons.activation.Expression;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.AbstractTrackedObject;

public class Parameter extends AbstractTrackedObject implements ActivableObject {
	
	protected String key;
	
	protected String value;
	
	protected String description;
	
	protected Expression activationExpression;
	
	protected Integer priority;
	
	protected Boolean protectedValue;
	
	protected ParameterScope scope;
	protected String scopeEntity;
	
	public Parameter() {
		super();
	}

	public Parameter(Expression activationExpression, String key, String value, String description) {
		super();
		this.key = key;
		this.value = value;
		this.description = description;
		this.activationExpression = activationExpression;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public Expression getActivationExpression() {
		return activationExpression;
	}

	@Override
	public Integer getPriority() {
		return priority;
	}
	
	public void setActivationExpression(Expression activationExpression) {
		this.activationExpression = activationExpression;
	}
	
	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getProtectedValue() {
		return protectedValue;
	}

	public void setProtectedValue(Boolean protectedValue) {
		this.protectedValue = protectedValue;
	}

	/**
	 * @return the {@link ParameterScope} of this parameter
	 */
	public ParameterScope getScope() {
		return scope;
	}

	public void setScope(ParameterScope scope) {
		this.scope = scope;
	}

	/**
	 * @return the name of the entity this parameter is restricted to. For instance: if the scope of a Parameter 
	 * is set to FUNCTION, the scopeEntity represent the name of the Function for which this parameter applies
	 */
	public String getScopeEntity() {
		return scopeEntity;
	}

	public void setScopeEntity(String scopeEntity) {
		this.scopeEntity = scopeEntity;
	}
}
