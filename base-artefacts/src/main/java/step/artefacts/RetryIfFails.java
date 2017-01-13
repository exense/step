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

import step.artefacts.handlers.RetryIfFailsHandler;
import step.commons.dynamicbeans.DynamicAttribute;
import step.core.artefacts.Artefact;
import step.core.artefacts.AbstractArtefact;

@Artefact(handler = RetryIfFailsHandler.class)
public class RetryIfFails extends AbstractArtefact {
	
	@DynamicAttribute
	String maxRetries;
	
	@DynamicAttribute
	String gracePeriod;
	
	public Integer getMaxRetries() {
		return maxRetries!=null&&maxRetries.length()>0?Integer.parseInt(maxRetries):2;
	}
	
	public void setMaxRetries(String maxRetries) {
		this.maxRetries = maxRetries;
	}

	public Integer getGracePeriod() {
		return gracePeriod!=null&&gracePeriod.length()>0?Integer.parseInt(gracePeriod):0;
	}
	
	public void setGracePeriod(String gracePeriod) {
		this.gracePeriod = gracePeriod;
	}

}
