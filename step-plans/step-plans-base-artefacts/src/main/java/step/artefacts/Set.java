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

import step.artefacts.reports.SetReportNode;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.dynamicbeans.DynamicValue;

@Artefact(report = SetReportNode.class, block=false)
public class Set extends AbstractArtefact {

	private DynamicValue<String> key = new DynamicValue<>("");
	
	private DynamicValue<String> value = new DynamicValue<>("");

	public DynamicValue<String> getKey() {
		return key;
	}

	public void setKey(DynamicValue<String> key) {
		this.key = key;
	}

	public DynamicValue<String> getValue() {
		return value;
	}

	public void setValue(DynamicValue<String> value) {
		this.value = value;
	}
}
