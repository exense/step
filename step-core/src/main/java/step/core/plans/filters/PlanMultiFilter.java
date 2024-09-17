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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlanMultiFilter extends PlanFilter {

    private final List<PlanFilter> planFilters;

    @JsonCreator
    public PlanMultiFilter(@JsonProperty("planFilters") List<PlanFilter> planFilters) {
        this.planFilters = planFilters;
    }

    public List<PlanFilter> getPlanFilters() {
        return planFilters;
    }

    public void add(PlanFilter filter){
        planFilters.add(filter);
    }

    @Override
    public boolean isSelected(Plan plan) {
        return planFilters == null || planFilters.stream().allMatch(pf -> pf.isSelected(plan));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlanMultiFilter that = (PlanMultiFilter) o;
        return CollectionUtils.isEqualCollection(planFilters, that.planFilters);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(planFilters);
    }
}
