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

import java.util.List;
import java.util.stream.Stream;

import step.core.accessors.Accessor;
import step.core.accessors.LayeredAccessor;
import step.core.collections.IndexField;

public class LayeredPlanAccessor extends LayeredAccessor<Plan> implements PlanAccessor {

	public LayeredPlanAccessor() {
		super();
	}

	public LayeredPlanAccessor(List<PlanAccessor> accessors) {
		super(accessors);
	}

	@Override
	public void createIndexIfNeeded(IndexField indexField) {
		Accessor<Plan> accessorForPersistence = getAccessorForPersistence();
		if (accessorForPersistence instanceof PlanAccessor) {
			((PlanAccessor) accessorForPersistence).createIndexIfNeeded(indexField);
		}
	}

	@Override
	public Stream<Plan> getVisiblePlans() {
		throw new RuntimeException("getVisiblePlans not implemented in layered accessor");
	}
}
