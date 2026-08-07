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
package step.automation.packages.yaml.mappers;

import step.automation.packages.mappers.AbstractFunctionToYamlMapper;
import step.automation.packages.mappers.interfaces.BusinessObjectToYamlMapping;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.core.plans.Plan;
import step.plans.parser.yaml.YamlPlan;
import step.plans.parser.yaml.YamlPlanReader;
import step.plugins.functions.types.CompositeFunction;
import step.plugins.functions.types.automation.YamlCompositeFunction;

@BusinessObjectToYamlMapping(sourceClass = CompositeFunction.class)
public class CompositeFunctionToYamlMapper extends AbstractFunctionToYamlMapper<CompositeFunction> {

    private final YamlPlanReader planReader;

    public CompositeFunctionToYamlMapper(YamlPlanReader planReader) {
        this.planReader = planReader;
    }

    @Override
    public YamlAutomationPackageKeyword toYamlObject(CompositeFunction compositeFunction) {

        YamlCompositeFunction yamlComposite = new YamlCompositeFunction();
        setCommonAttributes(compositeFunction, yamlComposite);

        Plan plan = compositeFunction.getPlan();
        if (plan != null) {
            YamlPlan yamlPlan = planReader.planToYamlPlan(compositeFunction.getPlan());
            yamlPlan.setName(null);
            yamlComposite.setPlan(yamlPlan);
        }

        return new YamlAutomationPackageKeyword(yamlComposite, null);
    }
}
