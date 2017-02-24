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
package step.artefacts;

import step.artefacts.handlers.CallFunctionHandler;
import step.artefacts.reports.CallFunctionReportNode;
import step.commons.dynamicbeans.DynamicAttribute;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.dynamicbeans.DynamicValue;

@Artefact(handler = CallFunctionHandler.class, report = CallFunctionReportNode.class)
public class CallFunction extends AbstractArtefact {
	
	@DynamicAttribute
	String function;
	
	@DynamicAttribute
	String functionId;
	
	DynamicValue<String> argument = new DynamicValue<>("{}");
	
	@DynamicAttribute
	String token;
	
	@DynamicAttribute
	String resultMap;

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

	public String getFunctionId() {
		return functionId;
	}

	public void setFunctionId(String functionId) {
		this.functionId = functionId;
	}

	public DynamicValue<String> getArgument() {
		return argument;
	}

	public void setArgument(DynamicValue<String> argument) {
		this.argument = argument;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getResultMap() {
		return resultMap;
	}

	public void setResultMap(String resultMap) {
		this.resultMap = resultMap;
	}
}
