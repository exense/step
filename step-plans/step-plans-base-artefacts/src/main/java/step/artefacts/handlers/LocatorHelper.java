package step.artefacts.handlers;

import step.automation.packages.AutomationPackageEntity;
import step.commons.activation.Activator;
import step.commons.activation.Expression;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.ExecutionContextBindings;
import step.functions.EvaluationExpression;

import javax.script.SimpleBindings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LocatorHelper {
    /**
     * Reorders and filters entities according to the current automation package and activation expression
     */
    public static <T extends AbstractOrganizableObject> List<T> prioritizeAndFilterApEntities(List<T> entities, Map<String, Object> bindings) {
        // reorder entities: entities from current AP have a priority
        List<T> entitiesFromSameAP = new ArrayList<>();
        List<T> entitiesActivatedExplicitly = new ArrayList<>();
        List<T> entitiesWithLowerPriority = new ArrayList<>();
        String planApId = (bindings != null) ? (String) bindings.get(ExecutionContextBindings.BINDING_AP) : null;
        for (T entity : entities) {
            // If we're executing a plan belonging to an AP, any matching entity from the same AP as the priority
            // activation expression is not checked since all entity from the AP have the same activation expression, it should
            // not impact the selection
            if (planApId != null) {
                String entityApId = entity.getCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID, String.class);
                if (Objects.equals(entityApId, planApId)) {
                    entitiesFromSameAP.add(entity);
                    break;
                }
            }
            if (evaluationExpressionIsDefined(entity)) {
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

    public static <T extends AbstractOrganizableObject> boolean evaluationExpressionIsDefined(T entity) {
        return (entity instanceof EvaluationExpression && ((EvaluationExpression) entity).getActivationExpression() != null);
    }

    public static <T extends AbstractOrganizableObject> boolean isActivated(Map<String, Object> bindings, T entity) {
        if (evaluationExpressionIsDefined(entity)) {
            Expression activationExpression = ((EvaluationExpression) entity).getActivationExpression();
            return Activator.evaluateActivationExpression(bindings == null ? null : new SimpleBindings(bindings), activationExpression, Activator.DEFAULT_SCRIPT_ENGINE);
        } else {
            return true;
        }
    }
}
