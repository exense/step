/*
 * Copyright (C) 2025, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

package step.artefacts.handlers;

import step.automation.packages.AutomationPackageEntity;
import step.commons.activation.Expression;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.ExecutionContextBindings;
import step.entities.activation.Activator;
import step.functions.EvaluationExpression;

import javax.script.SimpleBindings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ApEntitiesPrioritizer {

    private final Activator activator;

    public ApEntitiesPrioritizer(Activator activator) {
        this.activator = activator;
    }

    /**
     * Reorders and filters entities according to the current automation package and activation expression
     */
    public <T extends AbstractOrganizableObject> List<T> prioritizeAndFilterApEntities(List<T> entities, Map<String, Object> bindings) {
        // reorder entities: entities from current AP have a priority
        List<T> entitiesFromSameAP = new ArrayList<>();
        List<T> entitiesActivatedExplicitly = new ArrayList<>();
        List<T> entitiesWithLowerPriority = new ArrayList<>();
        String planApId = (bindings != null) ? (String) bindings.get(ExecutionContextBindings.BINDING_AP) : null;
        for (T entity : entities) {
            // If we're executing a plan or keyword belonging to an AP, any matching entity from the same AP as the priority
            // activation expression is not checked since all entity from the AP have the same activation expression, it should
            // not impact the selection
            if (planApId != null) {
                String entityApId = entity.getCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID, String.class);
                if (Objects.equals(entityApId, planApId)) {
                    entitiesFromSameAP.add(entity);
                    break;
                }
            }
            if (evaluationExprressionIsDefined(entity)) {
                if (isActivated(bindings, entity)) {
                    entitiesActivatedExplicitly.add(entity);
                }
            } else {
                entitiesWithLowerPriority.add(entity);
            }
        }

        List<T> orderedEntities = new ArrayList<>();
        orderedEntities.addAll(entitiesFromSameAP);
        orderedEntities.addAll(entitiesActivatedExplicitly);
        orderedEntities.addAll(entitiesWithLowerPriority);
        return orderedEntities;
    }

    private <T extends AbstractOrganizableObject> boolean evaluationExprressionIsDefined(T entity) {
        return (entity instanceof EvaluationExpression && ((EvaluationExpression) entity).getActivationExpression() != null);
    }

    private <T extends AbstractOrganizableObject> boolean isActivated(Map<String, Object> bindings, T entity) {
        if (evaluationExprressionIsDefined(entity)) {
            Expression activationExpression = ((EvaluationExpression) entity).getActivationExpression();
            return activator.evaluateActivationExpression(bindings, activationExpression);
        } else {
            return true;
        }
    }
}
