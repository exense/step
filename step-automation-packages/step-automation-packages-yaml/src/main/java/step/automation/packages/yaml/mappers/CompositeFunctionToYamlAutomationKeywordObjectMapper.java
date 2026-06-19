package step.automation.packages.yaml.mappers;

import step.automation.packages.StagingAutomationPackageContext;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.functions.Function;
import step.plans.parser.yaml.YamlPlan;
import step.plans.parser.yaml.YamlPlanReader;
import step.plugins.functions.types.CompositeFunction;
import step.plugins.functions.types.automation.YamlCompositeFunction;

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
@ObjectToYamlMapping(organizableObject = CompositeFunction.class)
public class CompositeFunctionToYamlAutomationKeywordObjectMapper extends FunctionToYamlAutomationKeywordObjectMapper<CompositeFunction> {

    private final YamlPlanReader planReader;
    private final StagingAutomationPackageContext stagingContext;

    public CompositeFunctionToYamlAutomationKeywordObjectMapper(YamlPlanReader planReader, StagingAutomationPackageContext stagingContext) {
        this.planReader = planReader;
        this.stagingContext = stagingContext;
    }

    @Override
    public YamlAutomationPackageKeyword getNewYamlObject(CompositeFunction compositeFunction) {

        YamlCompositeFunction yamlComposite = new YamlCompositeFunction();
        setCommonAtributes(compositeFunction, yamlComposite);

        YamlPlan plan = planReader.planToYamlPlan(compositeFunction.getPlan());
        plan.setName(null);
        yamlComposite.setPlan(plan);

        return new YamlAutomationPackageKeyword(yamlComposite, null);
    }

    @Override
    public Optional<CompositeFunction> getBusinessObject(YamlAutomationPackageKeyword yamlKeyword) {
        Function function = yamlKeyword.prepareKeyword(stagingContext);
        if (function instanceof CompositeFunction compositeFunction) {
            return Optional.of(compositeFunction);
        }
        return Optional.empty();
    }
}
