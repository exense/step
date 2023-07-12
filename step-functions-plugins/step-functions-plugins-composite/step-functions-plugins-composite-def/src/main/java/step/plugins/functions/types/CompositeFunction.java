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
package step.plugins.functions.types;

import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.core.plans.Plan;
import step.functions.Function;
import step.core.functions.ArtefactFunction;

public class CompositeFunction extends Function implements ArtefactFunction {

	protected Plan plan;
	
	public CompositeFunction() {
		super();
		executeLocally = true;
	}

	@EntityReference(type= EntityManager.recursive)
	public Plan getPlan() {
		return plan;
	}

	public void setPlan(Plan plan) {
		this.plan = plan;
	}
}
