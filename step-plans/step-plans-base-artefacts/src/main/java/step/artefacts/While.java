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

import step.automation.packages.AutomationPackageNamedEntity;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.dynamicbeans.DynamicValue;
import step.plans.parser.yaml.model.YamlArtefact;

@YamlArtefact
@AutomationPackageNamedEntity(name = "while")
@Artefact(name = "While")
public class While extends AbstractArtefact {

	private DynamicValue<Boolean> condition = new DynamicValue<>("", "");
	private DynamicValue<Boolean> postCondition = new DynamicValue<>("", "");
	DynamicValue<Long> pacing = new DynamicValue<Long>();
	DynamicValue<Long> timeout = new DynamicValue<Long>();
	DynamicValue<Integer> maxIterations = new DynamicValue<Integer>();
	
	public While() {
		super();
	}
	
	public While(String conditionExpr) {
		super();
		condition = new DynamicValue<>(conditionExpr, "");
		setPacing(new DynamicValue<>(0L));
		setTimeout(new DynamicValue<>(0L));
		setMaxIterations(new DynamicValue<>(0));
	}

	public DynamicValue<Boolean> getCondition() {
		return condition;
	}

	public void setCondition(DynamicValue<Boolean> condition) {
		this.condition = condition;
	}
	
	public DynamicValue<Boolean> getPostCondition() {
		return postCondition;
	}

	public void setPostCondition(DynamicValue<Boolean> postCondition) {
		this.postCondition = postCondition;
	}

	public DynamicValue<Long> getPacing() {
		return pacing;
	}

	public void setPacing(DynamicValue<Long> pacing) {
		this.pacing = pacing;
	}
	
	public DynamicValue<Long> getTimeout() {
		return timeout;
	}

	public void setTimeout(DynamicValue<Long> timeout) {
		this.timeout = timeout;
	}
	
	public DynamicValue<Integer> getMaxIterations() {
		return this.maxIterations;
	}

	public void setMaxIterations(DynamicValue<Integer> maxIterations) {
		this.maxIterations = maxIterations;
	}
	
}
