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

import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.framework.server.tables.AbstractTable;
import step.framework.server.tables.service.TableParameters;

import java.util.List;

public class PlanTable extends AbstractTable<Plan> {

	public PlanTable(Collection<Plan> collection) {
		super(collection, "plan-read", true);
	}

	@Override
	public List<Filter> getTableFilters(TableParameters tableParameters) {
		return List.of(Filters.equals("visible", true));
	}
}
