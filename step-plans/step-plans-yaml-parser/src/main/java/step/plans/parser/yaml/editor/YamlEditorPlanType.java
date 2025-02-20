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
package step.plans.parser.yaml.editor;

import org.bson.types.ObjectId;
import step.core.plans.*;
import step.plans.nl.RootArtefactType;
import step.plans.parser.yaml.YamlPlanReader;

public class YamlEditorPlanType implements PlanType<YamlEditorPlan> {

    private final PlanCompiler<YamlEditorPlan> compiler = new YamlEditorPlanTypeCompiler();

    public YamlEditorPlanType() {
    }

    @Override
    public Class<YamlEditorPlan> getPlanClass() {
        return YamlEditorPlan.class;
    }

    @Override
    public PlanCompiler<YamlEditorPlan> getPlanCompiler() {
        return compiler;
    }

    @Override
    public YamlEditorPlan newPlan(String template, String name) throws Exception {
        YamlEditorPlan yamlEditorPlan = new YamlEditorPlan();
        RootArtefactType type = RootArtefactType.valueOf(template);
        yamlEditorPlan.setType(type);
        yamlEditorPlan.setRoot(type.createRootArtefact());
        yamlEditorPlan.setSource("");
        if (name != null) {
            YamlPlanReader.setPlanName(yamlEditorPlan, name);
        }
        return yamlEditorPlan;
    }

    @Override
    public YamlEditorPlan clonePlan(YamlEditorPlan plan, boolean updateVisibility) {
        plan.setId(new ObjectId());
        if (updateVisibility) {
            plan.setCustomFields(null);
            plan.setVisible(true);
            if (plan.getRoot() != null) {
                // delete all custom attributes for all children to clean up attributes like "source" cloned from original plan
                plan.getRoot().deepCleanupAllCustomAttributes();
            }
        }
        return plan;
    }

    @Override
    public void onBeforeSave(YamlEditorPlan plan) {

    }

}
