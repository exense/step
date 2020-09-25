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

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.functions.Function;

@JsonTypeInfo(use=Id.CLASS,property="_class")
public class Plan extends AbstractOrganizableObject {

	protected AbstractArtefact root;
	
	protected Collection<Function> functions;
	
	protected Collection<Plan> subPlans;
	
	protected boolean visible = true;
	
	public Plan(AbstractArtefact root) {
		super();
		this.root = root;
	}

	public Plan() {
		super();
	}

	@EntityReference(type=EntityManager.recursive)
	public AbstractArtefact getRoot() {
		return root;
	}

	public void setRoot(AbstractArtefact root) {
		this.root = root;
	}
	
	public Collection<Function> getFunctions() {
		return functions;
	}

	public void setFunctions(Collection<Function> functions) {
		this.functions = functions;
	}

	public Collection<Plan> getSubPlans() {
		return subPlans;
	}

	public void setSubPlans(Collection<Plan> subPlans) {
		this.subPlans = subPlans;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
}
