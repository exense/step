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

import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.collections.IndexField;

import java.util.stream.Stream;

public class PlanAccessorImpl extends AbstractAccessor<Plan> implements PlanAccessor {

	public PlanAccessorImpl(Collection<Plan> collectionDriver) {
		super(collectionDriver);
	}

	@Override
	public void createIndexIfNeeded(IndexField indexField) {
		createOrUpdateIndex(indexField);
	}

	@Override
	public Stream<Plan> getVisiblePlans() {
		return collectionDriver.find(Filters.equals("visible", true), null, null, null, 0);
	}
}
