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

import step.artefacts.handlers.CallPlanHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.dynamicbeans.DynamicValue;

@Artefact(handler = CallPlanHandler.class)
public class CallPlan extends AbstractArtefact {
	
	private String planId;
	
	private DynamicValue<String> selectionAttributes = new DynamicValue<>("{}");
		
	DynamicValue<String> input = new DynamicValue<>("{}");
	
	public CallPlan() {
		super();
	}
	
	public boolean isCreateSkeleton() {
		return true;
	}

	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
	}

	public DynamicValue<String> getSelectionAttributes() {
		return selectionAttributes;
	}

	public void setSelectionAttributes(DynamicValue<String> selectionAttributes) {
		this.selectionAttributes = selectionAttributes;
	}

	public DynamicValue<String> getInput() {
		return input;
	}

	public void setInput(DynamicValue<String> input) {
		this.input = input;
	}
}
