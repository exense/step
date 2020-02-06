package step.core.plans;

import step.core.GlobalContext;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactRegistry;
import step.core.plans.builder.PlanBuilder;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin
public class PlanPlugin extends AbstractControllerPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		PlanTypeRegistry planTypeRegistry = new PlanTypeRegistry();
		planTypeRegistry.register(new PlanType<Plan>() {

			@Override
			public Class<Plan> getPlanClass() {
				return Plan.class;
			}

			@Override
			public PlanCompiler<Plan> getPlanCompiler() {
				return new PlanCompiler<Plan>() {
					@Override
					public Plan compile(Plan plan) {
						return plan;
					}
				};
			}

			@Override
			public Plan newPlan(String template) throws Exception {
				AbstractArtefact artefact = ArtefactRegistry.getInstance().getArtefactTypeInstance(template);
				Plan plan = PlanBuilder.create().startBlock(artefact).endBlock().build();
				return plan;
			}
		});
		context.put(PlanTypeRegistry.class, planTypeRegistry);
		context.getServiceRegistrationCallback().registerService(PlanServices.class);
	}
}
