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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.collections.CollectionUtils;
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.core.plans.PlanFilter;

import java.util.List;
import java.util.Objects;

public class PlanByIncludedNamesFilter extends PlanFilter {

    private final List<String> includedNames;

    @JsonCreator
    public PlanByIncludedNamesFilter(@JsonProperty("includedNames") List<String> includedNames) {
        this.includedNames = includedNames;
    }

    @Override
    public boolean isSelected(Plan plan) {
        return includedNames.contains(plan.getAttribute(AbstractOrganizableObject.NAME));
    }

    public List<String> getIncludedNames() {
        return includedNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlanByIncludedNamesFilter)) return false;
        PlanByIncludedNamesFilter that = (PlanByIncludedNamesFilter) o;
        return CollectionUtils.isEqualCollection(includedNames, that.includedNames);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(includedNames);
    }
}
