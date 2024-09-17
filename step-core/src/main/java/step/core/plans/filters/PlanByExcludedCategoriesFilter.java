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
import step.core.plans.Plan;
import step.core.plans.PlanFilter;

import java.util.List;
import java.util.Objects;

public class PlanByExcludedCategoriesFilter extends PlanFilter {

    private final List<String> excludeCategories;

    @JsonCreator
    public PlanByExcludedCategoriesFilter(@JsonProperty("excludedCategories") List<String> excludeCategories) {
        this.excludeCategories = excludeCategories;
    }

    public List<String> getExcludeCategories() {
        return excludeCategories;
    }

    /**
     * Determine whether the plan should be included. This filter select plans which belongs to one of the included categories
     * @param plan to be evaluated
     * @return whether this plan belong to one of the included categories
     */
    @Override
    public boolean isSelected(Plan plan) {
        return plan.getCategories() == null || excludeCategories.stream().noneMatch(plan.getCategories()::contains);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlanByExcludedCategoriesFilter)) return false;
        PlanByExcludedCategoriesFilter that = (PlanByExcludedCategoriesFilter) o;
        return CollectionUtils.isEqualCollection(excludeCategories, that.excludeCategories);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(excludeCategories);
    }
}
