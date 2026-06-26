package step.automation.packages.yaml.mappers;

import step.automation.packages.mappers.interfaces.BusinessObjectToYamlMapper;
import step.automation.packages.mappers.interfaces.BusinessObjectToYamlMapping;
import step.automation.packages.mappers.interfaces.YamlToBusinessObjectMapper;
import step.automation.packages.mappers.interfaces.YamlToBusinessObjectMapping;
import step.core.plans.Plan;
import step.plans.parser.yaml.YamlPlan;
import step.plans.parser.yaml.YamlPlanReader;

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

@BusinessObjectToYamlMapping(sourceClass = Plan.class)
@YamlToBusinessObjectMapping(sourceClass = YamlPlan.class)
public class PlanMapper implements BusinessObjectToYamlMapper<Plan, YamlPlan>,
    YamlToBusinessObjectMapper<Plan, YamlPlan> {

    private final YamlPlanReader planReader;

    public PlanMapper(YamlPlanReader planReader) {
        this.planReader = planReader;
    }

    @Override
    public YamlPlan getNewYamlObject(Plan plan) {
        return planReader.planToYamlPlan(plan);
    }

    @Override
    public Plan getBusinessObject(YamlPlan yamlPlan) {
        return planReader.yamlPlanToPlan(yamlPlan);
    }

    @Override
    public String getCollectionName() {
        return YamlPlan.PLANS_ENTITY_NAME;
    }

}
