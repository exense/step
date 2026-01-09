package step.core.plans;

import step.artefacts.CallPlan;
import step.artefacts.handlers.PlanLocator;
import step.core.accessors.Accessor;
import step.core.entities.Entity;
import step.core.entities.EntityConstants;
import step.core.entities.EntityManager;

public class PlanEntity extends Entity<Plan, Accessor<Plan>> {

    public PlanEntity(Accessor<Plan> accessor, PlanLocator planLocator, EntityManager entityManager) {
        super(EntityConstants.plans, accessor, Plan.class);
        entityManager.addDependencyTreeVisitorHook((entity, context) -> {
            //This is only required to recursively visit the plans referenced by callPlan artefacts
            if (entity instanceof CallPlan && context.isRecursive()) {
                try {
                    Plan plan = planLocator.selectPlanNotNull((CallPlan) entity, context.getObjectPredicate(), null);
                    context.visitEntity(EntityConstants.plans, plan.getId().toString());
                } catch (PlanLocator.PlanLocatorException ex) {
                    context.getVisitor().onWarning(ex.getMessage());
                }
            }
        });
    }
}
