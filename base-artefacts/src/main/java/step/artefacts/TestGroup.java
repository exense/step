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

import step.artefacts.handlers.TestGroupHandler;
import step.commons.dynamicbeans.DynamicAttribute;
import step.core.artefacts.Artefact;
import step.core.artefacts.AbstractArtefact;

@Artefact(handler = TestGroupHandler.class)
public class TestGroup extends AbstractArtefact {
	
	@DynamicAttribute
	private String rampup = null;
	
	@DynamicAttribute
	private String pacing = null;
	
	@DynamicAttribute
	private String users = "1";
	
	@DynamicAttribute
	private String iterations = "1";

	@DynamicAttribute
	private String startOffset = "0";

	public String getRampup() {
		return rampup;
	}

	public void setRampup(String rampup) {
		this.rampup = rampup;
	}

	public String getPacing() {
		return pacing;
	}

	public void setPacing(String pacing) {
		this.pacing = pacing;
	}

	public String getUsers() {
		return users;
	}

	public void setUsers(String users) {
		this.users = users;
	}

	public String getIterations() {
		return iterations;
	}

	public void setIterations(String iterations) {
		this.iterations = iterations;
	}

	public String getStartOffset() {
		return startOffset;
	}

	public void setStartOffset(String startOffset) {
		this.startOffset = startOffset;
	}

}
