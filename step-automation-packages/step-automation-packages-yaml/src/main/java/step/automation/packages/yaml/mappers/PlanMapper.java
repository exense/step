package step.automation.packages.yaml.mappers;

import step.artefacts.CallPlan;
import step.artefacts.automation.YamlCallPlan;
import step.automation.packages.mappers.interfaces.BusinessObjectToYamlMapper;
import step.automation.packages.mappers.interfaces.BusinessObjectToYamlMapping;
import step.automation.packages.mappers.interfaces.ReferenceHandlingObjectMapper;
import step.automation.packages.mappers.interfaces.YamlToBusinessObjectMapper;
import step.automation.packages.mappers.interfaces.YamlToBusinessObjectMapping;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;
import step.core.yaml.model.NamedYamlArtefact;
import step.plans.parser.yaml.YamlPlan;
import step.plans.parser.yaml.YamlPlanReader;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
@YamlToBusinessObjectMapping
public class PlanMapper implements BusinessObjectToYamlMapper<Plan, YamlPlan>,
    YamlToBusinessObjectMapper<YamlPlan, Plan>,
    ReferenceHandlingObjectMapper<Plan> {

    private final YamlPlanReader planReader;

    private final Map<String, Plan> idToPlanMap = new HashMap<>();
    private final Map<Plan, Set<Plan>> planToReferencingPlansMap = new HashMap<>();

    public PlanMapper(YamlPlanReader planReader) {
        this.planReader = planReader;
    }

    @Override
    public YamlPlan toYamlObject(Plan plan) {
        YamlPlan yamlPlan = planReader.planToYamlPlan(plan);
        setNameReferences(yamlPlan.getRoot());
        return yamlPlan;
    }

    private void setNameReferences(NamedYamlArtefact node) {
        node.getYamlArtefact().getChildren().forEach(this::setNameReferences);

        if (node.getYamlArtefact() instanceof YamlCallPlan yamlCallPlan) {
            Plan plan = idToPlanMap.get(yamlCallPlan.getPlanId());
            if (plan != null) {
                yamlCallPlan.setPlan(plan.getAttribute(AbstractOrganizableObject.NAME));
                yamlCallPlan.setPlanId(null);
            }
        }
    }

    @Override
    public Plan toBusinessObject(YamlPlan yamlPlan) {
        Plan plan = planReader.yamlPlanToPlan(yamlPlan);
        idToPlanMap.put(plan.getId().toString(), plan);
        return plan;
    }

    @Override
    public String getCollectionName() {
        return YamlPlan.PLANS_ENTITY_NAME;
    }

    @Override
    public void setReferences() {
        Map<String, Plan> nameToPlanMap = new HashMap<>();
        idToPlanMap.values().forEach(p -> nameToPlanMap.put(p.getAttribute(AbstractOrganizableObject.NAME), p));
        idToPlanMap.values().forEach(p -> setReferences(nameToPlanMap, p, p.getRoot()));
    }

    private void setReferences(Map<String, Plan> nameToPlanMap, Plan plan, AbstractArtefact node) {
        node.getChildren().forEach(a -> setReferences(nameToPlanMap, plan, a));

        if (node instanceof CallPlan callPlan) {
            Plan referencedPlan = nameToPlanMap.get(callPlan.getPlan());
            planToReferencingPlansMap.computeIfAbsent(referencedPlan, p -> new HashSet<>()).add(plan);
            if (plan != null) {
                callPlan.setPlanId(plan.getId().toString());
                callPlan.setPlan(null);
            }
        }
    }

    @Override
    public Collection<Plan> getReferrers(Plan plan) {
        return planToReferencingPlansMap.get(plan);
    }
}
