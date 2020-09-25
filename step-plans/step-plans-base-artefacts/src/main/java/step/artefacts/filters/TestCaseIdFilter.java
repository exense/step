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
package step.artefacts.filters;

import java.util.ArrayList;
import java.util.List;

import step.artefacts.TestCase;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactFilter;

public class TestCaseIdFilter extends ArtefactFilter {
	
	private List<String> includedIds = new ArrayList<>();

	public TestCaseIdFilter() {
		super();
	}

	public TestCaseIdFilter(List<String> includedIds) {
		super();
		this.includedIds = includedIds;
	}

	@Override
	public boolean isSelected(AbstractArtefact artefact) {
		if(artefact instanceof TestCase) {
			return artefact.getId()!=null?includedIds.contains(artefact.getId().toString()):false;
		} else {
			return true;
		}
	}

	public List<String> getIncludedIds() {
		return includedIds;
	}

	public void setIncludedIds(List<String> includedIds) {
		this.includedIds = includedIds;
	}

}
