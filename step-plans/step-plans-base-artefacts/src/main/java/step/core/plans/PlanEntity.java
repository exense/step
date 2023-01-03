package step.core.plans;

import step.artefacts.CallPlan;
import step.artefacts.handlers.PlanLocator;
import step.core.accessors.Accessor;
import step.core.entities.Entity;
import step.core.entities.EntityManager;

public class PlanEntity extends Entity<Plan, Accessor<Plan>> {

    public PlanEntity(Accessor<Plan> accessor, PlanLocator planLocator, EntityManager entityManager) {
        super(EntityManager.plans, accessor, Plan.class);
        entityManager.addDependencyTreeVisitorHook((entity, context) -> {
            if (entity instanceof CallPlan) {
                try {
                    Plan plan = planLocator.selectPlanNotNull((CallPlan) entity, context.getObjectPredicate(), null);
                    context.visitEntity(EntityManager.plans, plan.getId().toString());
                } catch (PlanLocator.PlanLocatorException ex) {
                    // just skip missing plan during import, but add error message to import result
                    if (context.getMessageCollector() != null) {
                        context.getMessageCollector().add(ex.getMessage());
                    }
                }
            }
        });
    }
}
