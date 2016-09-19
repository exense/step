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
package step.artefacts.filters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import step.artefacts.TestCase;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactFilter;

public class TestCaseFilter extends ArtefactFilter {
	
	private List<String> includedNames = new ArrayList<>();

	public TestCaseFilter() {
		super();
	}

	public TestCaseFilter(List<String> includedNames) {
		super();
		this.includedNames = includedNames;
	}

	@Override
	public boolean isSelected(AbstractArtefact artefact) {
		if(artefact instanceof TestCase) {
			Map<String, String> attributes = new HashMap<>();
			attributes = artefact.getAttributes();
			String name = attributes.get("name");
			return name!=null?includedNames.contains(name):false;
		} else {
			return true;
		}
	}

	public List<String> getIncludedNames() {
		return includedNames;
	}

	public void setIncludedNames(List<String> includedNames) {
		this.includedNames = includedNames;
	}

}
