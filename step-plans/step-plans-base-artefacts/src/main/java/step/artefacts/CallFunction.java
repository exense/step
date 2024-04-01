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
package step.artefacts;

import step.artefacts.automation.YamlCallFunction;
import step.artefacts.reports.CallFunctionReportNode;
import step.automation.packages.AutomationPackageNamedEntity;
import step.core.artefacts.Artefact;
import step.core.dynamicbeans.DynamicValue;
import step.plans.parser.yaml.model.YamlArtefact;

@YamlArtefact(model = YamlCallFunction.class)
@AutomationPackageNamedEntity(name = "callKeyword")
@Artefact(name=CallFunction.ARTEFACT_NAME, report = CallFunctionReportNode.class)
public class CallFunction extends TokenSelector {
	
	public static final String ARTEFACT_NAME = "CallKeyword";

	private DynamicValue<String> function = new DynamicValue<>("{}");

	private DynamicValue<String> argument = new DynamicValue<>("{}");
	private DynamicValue<String> resultMap = new DynamicValue<>();

	public DynamicValue<String> getFunction() {
		return function;
	}

	public void setFunction(DynamicValue<String> function) {
		this.function = function;
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
