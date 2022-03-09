package step.core.repositories;

import step.core.execution.ExecutionContext;
import step.core.plans.Plan;

public abstract class AbstractRepository implements Repository {

    protected void enrichPlan(ExecutionContext context, Plan plan) {
        context.getObjectEnricher().accept(plan);
    }
}
