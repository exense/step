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

import step.artefacts.handlers.SleepHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.dynamicbeans.DynamicValue;

@Artefact(handler = SleepHandler.class, block=false)
public class Sleep extends AbstractArtefact {

	private DynamicValue<Long> duration = new DynamicValue<>(0L);
	private DynamicValue<String> unit = new DynamicValue<>("ms");
	private DynamicValue<Boolean> releaseTokens = new DynamicValue<>(false);

	public DynamicValue<Long> getDuration() {
		return duration;
	}

	public void setDuration(DynamicValue<Long> duration) {
		this.duration = duration;
	}

	public DynamicValue<Boolean> getReleaseTokens() {
		return releaseTokens;
	}

	public void setReleaseTokens(DynamicValue<Boolean> releaseTokens) {
		this.releaseTokens = releaseTokens;
	}

	public DynamicValue<String> getUnit() {
		return unit;
	}

	public void setUnit(DynamicValue<String> unit) {
		this.unit = unit;
	}
}
