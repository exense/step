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
package step.core.plans;

import step.core.artefacts.AbstractArtefact;

public class PlanNavigator {

	protected final Plan plan;
	
	public PlanNavigator(Plan plan) {
		super();
		this.plan = plan;
	}

	public AbstractArtefact findArtefactById(String id) {
		return findArtefactByIdRecursive(id, plan.getRoot());
	}
	
	protected AbstractArtefact findArtefactByIdRecursive(String id, AbstractArtefact a) {
		if(a.getId().toString().equals(id)) {
			return a;
		} else {
			for (AbstractArtefact child : a.getChildren()) {
				AbstractArtefact result = findArtefactByIdRecursive(id, child);
				if(result != null) {
					return result;
				}
			}
			return null;
		}
	}
	
}
