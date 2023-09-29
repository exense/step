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

@Artefact(validAsRoot = true)
public class Sequence extends AbstractArtefact {
	
	DynamicValue<Boolean> continueOnError = new DynamicValue<Boolean>(false);
	
	DynamicValue<Long> pacing = new DynamicValue<Long>();

	public DynamicValue<Boolean> getContinueOnError() {
		return continueOnError;
	}

	public void setContinueOnError(DynamicValue<Boolean> continueOnError) {
		this.continueOnError = continueOnError;
	}

	public DynamicValue<Long> getPacing() {
		return pacing;
	}

	public void setPacing(DynamicValue<Long> pacing) {
		this.pacing = pacing;
	}
	
}
