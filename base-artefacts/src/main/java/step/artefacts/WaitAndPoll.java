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

import step.artefacts.handlers.WaitAndPollHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.dynamicbeans.DynamicValue;

@Artefact(name = "WaitAndPoll", handler = WaitAndPollHandler.class)
public class WaitAndPoll extends AbstractArtefact {

	// Choose desired status (not necessarily PASSED)
	DynamicValue<Long> pacing = new DynamicValue<Long>();
	DynamicValue<Long> timeout = new DynamicValue<Long>();
	DynamicValue<Integer> maxIterations = new DynamicValue<Integer>();
	
	public WaitAndPoll() {
		super();
	}
	
	public WaitAndPoll(String conditionExpr) {
		super();
		setPacing(new DynamicValue<>(0L));
		setTimeout(new DynamicValue<>(0L));
		setMaxIterations(new DynamicValue<>(0));
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
