package step.core.scheduler;

import step.core.accessors.Accessor;
import step.core.entities.DependencyTreeVisitorHook;
import step.core.entities.Entity;
import step.core.entities.EntityDependencyTreeVisitor;
import step.core.entities.EntityManager;
import step.core.repositories.RepositoryObjectReference;

import static step.core.repositories.RepositoryObjectReference.LOCAL_REPOSITORY_ID;

public class ScheduleEntity extends Entity<ExecutiontTaskParameters, Accessor<ExecutiontTaskParameters>> {


    public ScheduleEntity(Accessor<ExecutiontTaskParameters> accessor, Class<ExecutiontTaskParameters> entityClass, EntityManager entityManager) {
        super(EntityManager.tasks, accessor, entityClass);
        entityManager.addDependencyTreeVisitorHook(scheduleReferencesHook(entityManager));
    }

    private DependencyTreeVisitorHook scheduleReferencesHook(EntityManager entityManager) {
        return new DependencyTreeVisitorHook() {
            @Override
            public void onVisitEntity(Object entity, EntityDependencyTreeVisitor.EntityTreeVisitorContext context) {
                if (entity instanceof ExecutiontTaskParameters) {
                    ExecutiontTaskParameters schedule = (ExecutiontTaskParameters) entity;
                    RepositoryObjectReference repositoryObject = schedule.getExecutionsParameters().getRepositoryObject();
                    if (repositoryObject != null && repositoryObject.getRepositoryID() != null && repositoryObject.getRepositoryID().equals(LOCAL_REPOSITORY_ID)) {
                        String localPlanId = repositoryObject.getRepositoryParameters().get(RepositoryObjectReference.PLAN_ID);
                        if (localPlanId != null) {
                            if (context.isRecursive()) {
                                context.visitEntity(EntityManager.plans, localPlanId);
                            }
                            String newEntityId = context.resolvedEntityId(EntityManager.plans, localPlanId);
                            if(newEntityId != null) {
                                repositoryObject.getRepositoryParameters().put(RepositoryObjectReference.PLAN_ID, newEntityId);
                            }
                        }
                    }
                }
            }
        };
    }

}
