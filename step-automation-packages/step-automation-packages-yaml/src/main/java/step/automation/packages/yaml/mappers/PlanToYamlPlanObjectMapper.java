package step.automation.packages.yaml.mappers;

import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.core.plans.Plan;
import step.core.yaml.deserialization.PatchableYamlList;
import step.plans.parser.yaml.YamlPlan;
import step.plans.parser.yaml.YamlPlanReader;

import java.util.Optional;

/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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

@ObjectToYamlMapping(organizableObject = Plan.class)
public class PlanToYamlPlanObjectMapper extends ObjectToYamlObjectMapper<Plan, YamlPlan> {

    private final YamlPlanReader planReader;

    public PlanToYamlPlanObjectMapper(YamlPlanReader planReader) {
        this.planReader = planReader;
    }

    @Override
    public YamlPlan getNewYamlObject(Plan plan) {
        return planReader.planToYamlPlan(plan);
    }

    @Override
    public Optional<Plan> getBusinessObject(YamlPlan yamlPlan) {
        return Optional.ofNullable(planReader.yamlPlanToPlan(yamlPlan));
    }

    @Override
    public PatchableYamlList<YamlPlan> getListInFragment(AutomationPackageFragmentYaml fragment) {
        return fragment.getPlans();
    }

    @Override
    public String getCollectionName() {
        return YamlPlan.PLANS_ENTITY_NAME;
    }

}
