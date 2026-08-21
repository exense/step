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
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.yaml.model.NamedYamlArtefact;
import step.plans.parser.yaml.YamlPlan;
import step.plans.parser.yaml.YamlPlanReader;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    ReferenceHandlingObjectMapper {

    private final YamlPlanReader planReader;

    private final Map<String, AbstractOrganizableObject> idToObjectMap = new HashMap<>();
    private final Map<String, Set<AbstractOrganizableObject>> idToReferencingObjectMap = new HashMap<>();

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
            AbstractOrganizableObject plan = idToObjectMap.get(yamlCallPlan.getPlanId());
            if (plan != null) {
                yamlCallPlan.setPlan(plan.getAttribute(AbstractOrganizableObject.NAME));
                yamlCallPlan.setPlanId(null);
            }
        }
    }

    @Override
    public Plan toBusinessObject(YamlPlan yamlPlan) {
        Plan plan = planReader.yamlPlanToPlan(yamlPlan);
        idToObjectMap.put(plan.getId().toString(), plan);
        return plan;
    }

    @Override
    public String getCollectionName() {
        return YamlPlan.PLANS_ENTITY_NAME;
    }

    @Override
    public void updateReferences(AbstractOrganizableObject object) {
        idToReferencingObjectMap.clear();
        setReferences();
    }

    @Override
    public void setReferences() {
        Map<String, String> nameToIdMap = idToObjectMap.values().stream()
            .collect(Collectors.toMap(
                p -> p.getAttribute(AbstractOrganizableObject.NAME),
                p -> p.getId().toString()));
        idToObjectMap.values().forEach(p -> {
            if (p instanceof Plan plan) {
                setReferences(nameToIdMap, plan, plan.getRoot());
            }
        });
    }

    private void setReferences(Map<String, String> nameToIdMap, Plan plan, AbstractArtefact node) {
        node.getChildren().forEach(a -> setReferences(nameToIdMap, plan, a));

        if (node instanceof CallPlan callPlan) {
            String planName = callPlan.getPlan();
            String planId = callPlan.getPlanId();
            if ((planName != null && !planName.trim().isEmpty()) || planId != null) {
                String referencedPlanId = nameToIdMap.getOrDefault(planName, planId);
                if (referencedPlanId != null) {
                    idToReferencingObjectMap.computeIfAbsent(referencedPlanId, p -> new HashSet<>()).add(plan);
                    callPlan.setPlanId(referencedPlanId);
                    callPlan.setPlan(null);
                } else {
                    callPlan.setSelectionAttributes(new DynamicValue<>("{\"name\": {\"value\": \"" + callPlan.getPlan() + "\", \"dynamic\": false}}"));
                    callPlan.setPlan(null);
                }
            }
        }
    }

    @Override
    public Collection<AbstractOrganizableObject> getReferrers(AbstractOrganizableObject plan) {
        idToObjectMap.put(plan.getId().toString(), plan);
        return idToReferencingObjectMap.getOrDefault(plan.getId().toString(), Collections.emptySet());
    }

    @Override
    public void removeReferences(AbstractOrganizableObject object) {
        idToObjectMap.remove(object.getId().toString());
        idToReferencingObjectMap.clear();
        setReferences();
    }
}
