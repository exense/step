package step.core.references;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.entities.EntityDependencyTreeVisitor;
import step.core.entities.EntityManager;

import java.util.Set;

public class FindReferencesTreeVisitor implements EntityDependencyTreeVisitor.EntityTreeVisitor {

    private static final Logger logger = LoggerFactory.getLogger(FindReferencesTreeVisitor.class);

    private final Set<Object> referencedObjects;
    private final EntityManager entityManager;

    public FindReferencesTreeVisitor(EntityManager entityManager, Set<Object> referencedObjects) {
        this.entityManager = entityManager;
        this.referencedObjects = referencedObjects;
    }

    @Override
    public void onWarning(String warningMessage) {
        logger.warn(warningMessage);
    }

    @Override
    public void onResolvedEntity(String entityName, String entityId, Object entity) {
        referencedObjects.add(entity);
    }

    @Override
    public String onResolvedEntityId(String entityName, String entityId) {
        if (entityId != null && entityName != null) {
            try {
                Object entity = entityManager.getEntityByName(entityName).getAccessor().get(entityId);
                onResolvedEntity(entityName, entityId, entity);
            } catch (Exception e) {
                logger.error("Unable to resolve entity: " + entityName + "/" + entityId, e);
            }
        }
        return null;
    }
}
