package step.core.references;

import step.core.entities.EntityDependencyTreeVisitor;
import step.core.plans.Plan;

import java.util.Set;

public class FindReferencesTreeVisitor implements EntityDependencyTreeVisitor.EntityTreeVisitor {

    private final Set<Object> referencedObjects;

    public FindReferencesTreeVisitor(Set<Object> referenced) {
        this.referencedObjects = referenced;
    }

    @Override
    public void onWarning(String warningMessage) {

    }

    @Override
    public void onResolvedEntity(String entityName, String entityId, Object entity) {
        if (entity instanceof Plan) {
            Plan plan = (Plan) entity;
            referencedObjects.add(plan);
        } else {
            System.err.println("onResolvedEntity UNKNOWN" + entityName + " " + entityId + " " + entity.getClass());
        }
    }

    @Override
    public String onResolvedEntityId(String entityName, String entityId) {
        return null;
    }
}
