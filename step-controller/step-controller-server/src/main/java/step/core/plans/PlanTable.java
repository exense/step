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

import javax.json.JsonObject;

import ch.exense.commons.core.collections.Collection;
import ch.exense.commons.core.collections.Filter;
import ch.exense.commons.core.collections.Filters;
import step.core.tables.AbstractTable;

public class PlanTable extends AbstractTable<Plan> {

	public PlanTable(Collection<Plan> collection) {
		super(collection, true);
	}

	@Override
	public List<Filter> getAdditionalQueryFragments(JsonObject queryParameters) {
		return List.of(Filters.equals("visible", true));
	}
}
