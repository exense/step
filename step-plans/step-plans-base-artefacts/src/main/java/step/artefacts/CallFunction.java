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

import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.Artefact;
import step.core.dynamicbeans.DynamicValue;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;

@Artefact(name=CallFunction.ARTEFACT_NAME, report = CallFunctionReportNode.class)
public class CallFunction extends TokenSelector {
	
	public static final String ARTEFACT_NAME = "CallKeyword";

	String functionId;
	
	DynamicValue<String> function = new DynamicValue<>("{}");
	
	DynamicValue<String> argument = new DynamicValue<>("{}");
	
	DynamicValue<String> resultMap = new DynamicValue<String>();

	public DynamicValue<String> getFunction() {
		return function;
	}

	public void setFunction(DynamicValue<String> function) {
		this.function = function;
	}

	@EntityReference(type=EntityManager.functions)
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

	public DynamicValue<String> getResultMap() {
		return resultMap;
	}

	public void setResultMap(DynamicValue<String> resultMap) {
		this.resultMap = resultMap;
	}
}
