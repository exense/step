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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import step.automation.packages.StagingAutomationPackageContext;
import step.automation.packages.model.AbstractYamlFunction;
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.core.yaml.YamlFieldCustomCopy;
import step.core.yaml.YamlModel;
import step.core.yaml.model.AbstractYamlArtefact;
import step.core.yaml.model.NamedYamlArtefact;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.functions.Function;
import step.jsonschema.JsonSchema;
import step.plans.parser.yaml.YamlPlan;
import step.plugins.functions.types.CompositeFunction;

import java.util.Map;
import java.util.Objects;

@YamlModel(name = "Composite")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class YamlCompositeFunction extends AbstractYamlFunction<CompositeFunction> {

    @YamlFieldCustomCopy
    @JsonSchema(ref = YamlJsonSchemaHelper.DEFS_PREFIX + YamlJsonSchemaHelper.COMPOSITE_PLAN_DEF)
    private YamlPlan plan = null;

    public YamlPlan getPlan() {
        return plan;
    }

    public void setPlan(YamlPlan plan) {
        this.plan = plan;
    }

    public YamlCompositeFunction() {
        super();

        // executeLocally is true by default
        this.setExecuteLocally(true);
    }

    @Override
    protected void fillDeclaredFields(CompositeFunction res, StagingAutomationPackageContext context) {
        super.fillDeclaredFields(res, context);
        if (plan != null) {
            res.setPlan(yamlPlanToPlan(plan));
        }
    }

    @Override
    public void updateFromFunction(Function function) {
        copyFieldsFromObject(function, false);

        if (function instanceof CompositeFunction) {
            Plan plan = ((CompositeFunction) function).getPlan();
            // plan name is optional, the composite function name is used by default
            if (this.plan.getName() != null && !this.plan.getName().isEmpty()) {
                this.plan.setName(plan.getAttribute(AbstractOrganizableObject.NAME));;
            }
            ObjectMapper mapper = this.plan.getRoot().getYamlArtefact().getYamlObjectMapper();
            this.plan.setRoot(new NamedYamlArtefact(AbstractYamlArtefact.toYamlArtefact(plan.getRoot(), mapper)));
        }
    }


    private Plan yamlPlanToPlan(YamlPlan yamlPlan) {
        Plan plan = new Plan(yamlPlan.getRoot().getYamlArtefact().toArtefact());

        // plan name is optional, the composite function name is used by default
        if (yamlPlan.getName() != null && !yamlPlan.getName().isEmpty()) {
            plan.addAttribute(AbstractOrganizableObject.NAME, yamlPlan.getName());
        } else {
            plan.addAttribute(AbstractOrganizableObject.NAME, getName());
        }
        return plan;
    }

    @Override
    protected CompositeFunction createFunctionInstance() {
        return new CompositeFunction();
    }

}
