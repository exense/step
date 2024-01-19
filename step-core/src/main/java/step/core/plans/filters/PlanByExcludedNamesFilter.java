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
package step.core.plans.filters;

import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.core.plans.PlanFilter;

import java.util.ArrayList;
import java.util.List;

public class PlanByExcludedNamesFilter extends PlanFilter {

    private List<String> excludedNames = new ArrayList<>();

    public PlanByExcludedNamesFilter() {
    }

    public PlanByExcludedNamesFilter(List<String> excludedNames) {
        this.excludedNames = excludedNames;
    }

    @Override
    public boolean isSelected(Plan plan) {
        return !excludedNames.contains(plan.getAttribute(AbstractOrganizableObject.NAME));
    }

    public List<String> getExcludedNames() {
        return excludedNames;
    }

    public void setExcludedNames(List<String> excludedNames) {
        this.excludedNames = excludedNames;
    }
}
