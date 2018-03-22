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
import step.core.accessors.AbstractDBObject;

public class Parameter extends AbstractDBObject implements ActivableObject {
	
	String key;
	
	String value;
	
	String description;
	
	Expression activationExpression;
	
	Integer priority;
	
	Boolean protectedValue;
	
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
}
