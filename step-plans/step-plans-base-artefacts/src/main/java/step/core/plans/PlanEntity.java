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
            // Only apply the logic for CallPlan artifacts
            if (entity instanceof CallPlan) {
                CallPlan callPlan = (CallPlan) entity;
                switch (context.getVisitMode()) {
                    case RECURSIVE:
                        // In recursive mode, we recursively visit the resolved plan. If multiple plans match, the one with the highest priority is chosen
                        try {
                            Plan plan = planLocator.selectPlanNotNull(callPlan, context.getObjectPredicate(), null);
                            context.visitEntity(EntityConstants.plans, plan.getId().toString());
                        } catch (PlanLocator.PlanLocatorException ex) {
                            context.getVisitor().onWarning(ex.getMessage());
                        }
                        break;
                    case RESOLVE_ALL:
                        // In resolve ALL mode, we resolve all matching plans but do not visit recursively
                        planLocator.getMatchingPlans(callPlan, context.getObjectPredicate(), null).forEach(p -> {
                            context.onResolvedEntity(EntityConstants.plans, p.getId().toHexString(), p);
                        });
                        break;
                }
            }
        });
    }
}
