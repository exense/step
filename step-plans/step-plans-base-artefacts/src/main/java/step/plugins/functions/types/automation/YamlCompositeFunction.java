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
package step.plugins.functions.types.automation;

import step.automation.packages.AutomationPackageContext;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.model.AbstractYamlFunction;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.yaml.YamlFieldCustomCopy;
import step.core.yaml.YamlModel;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.jsonschema.JsonSchema;
import step.plans.parser.yaml.YamlPlan;
import step.plugins.functions.types.CompositeFunction;

import java.util.Map;
import java.util.Objects;

@YamlModel(name = "Composite")
public class YamlCompositeFunction extends AbstractYamlFunction<CompositeFunction> {

    @YamlFieldCustomCopy
    @JsonSchema(ref = YamlJsonSchemaHelper.DEFS_PREFIX + YamlJsonSchemaHelper.PLAN_DEF)
    private YamlPlan plan = null;

    public YamlPlan getPlan() {
        return plan;
    }

    public void setPlan(YamlPlan plan) {
        this.plan = plan;
    }

    @Override
    protected void fillDeclaredFields(CompositeFunction res, AutomationPackageContext context) {
        super.fillDeclaredFields(res, context);
        if (plan != null) {
            res.setPlan(yamlPlanToPlan(plan));
        }
    }

    // TODO: the same is implemented in YamlPlanReader - duplicated code
    public Plan yamlPlanToPlan(YamlPlan yamlPlan) {
        Plan plan = new Plan(yamlPlan.getRoot().getYamlArtefact().toArtefact());
        plan.addAttribute(AbstractOrganizableObject.NAME, yamlPlan.getName());
        return plan;
    }

    @Override
    protected CompositeFunction createFunctionInstance() {
        return new CompositeFunction();
    }

}
