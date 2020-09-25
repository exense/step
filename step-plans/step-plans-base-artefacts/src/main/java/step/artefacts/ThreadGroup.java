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

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.dynamicbeans.DynamicValue;

@Artefact(useAsTemplate = true)
public class ThreadGroup extends AbstractArtefact {
	
	DynamicValue<Integer> users = new DynamicValue<Integer>(1);

	DynamicValue<Integer> iterations = new DynamicValue<Integer>(1);
	
	DynamicValue<Integer> rampup = new DynamicValue<Integer>(null);
	
	DynamicValue<Integer> pacing = new DynamicValue<Integer>(null);

	DynamicValue<Integer> startOffset = new DynamicValue<Integer>(0);
	
	DynamicValue<Integer> maxDuration = new DynamicValue<Integer>(0);
	
	DynamicValue<String> item = new DynamicValue<String>("gcounter");
	
	DynamicValue<String> localItem = new DynamicValue<String>("literationId");

	DynamicValue<String> userItem = new DynamicValue<String>("userId");

	public DynamicValue<Integer> getUsers() {
		return users;
	}

	public void setUsers(DynamicValue<Integer> users) {
		this.users = users;
	}

	public DynamicValue<Integer> getIterations() {
		return iterations;
	}

	public void setIterations(DynamicValue<Integer> iterations) {
		this.iterations = iterations;
	}

	public DynamicValue<Integer> getRampup() {
		return rampup;
	}

	public void setRampup(DynamicValue<Integer> rampup) {
		this.rampup = rampup;
	}

	public DynamicValue<Integer> getPacing() {
		return pacing;
	}

	public void setPacing(DynamicValue<Integer> pacing) {
		this.pacing = pacing;
	}

	public DynamicValue<Integer> getStartOffset() {
		return startOffset;
	}

	public void setStartOffset(DynamicValue<Integer> startOffset) {
		this.startOffset = startOffset;
	}

	public DynamicValue<Integer> getMaxDuration() {
		return maxDuration;
	}

	public void setMaxDuration(DynamicValue<Integer> maxDuration) {
		this.maxDuration = maxDuration;
	}

	public DynamicValue<String> getItem() {
		return item;
	}

	public void setItem(DynamicValue<String> item) {
		this.item = item;
	}

	public DynamicValue<String> getLocalItem() {
		return localItem;
	}

	public void setLocalItem(DynamicValue<String> localItem) {
		this.localItem = localItem;
	}

	public DynamicValue<String> getUserItem() {
		return userItem;
	}

	public void setUserItem(DynamicValue<String> userItem) {
		this.userItem = userItem;
	}
	

}
