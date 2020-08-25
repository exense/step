package step.core.plans;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bson.types.ObjectId;

import step.core.GlobalContext;
import step.core.accessors.collections.CollectionRegistry;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.builder.PlanBuilder;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.plugins.screentemplating.Input;
import step.plugins.screentemplating.InputType;
import step.plugins.screentemplating.ScreenInput;
import step.plugins.screentemplating.ScreenInputAccessor;
import step.plugins.screentemplating.ScreenTemplatePlugin;

@Plugin(dependencies= {ScreenTemplatePlugin.class})
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
				AbstractArtefact artefact = context.getArtefactHandlerRegistry().getArtefactTypeInstance(template);
				Plan plan = PlanBuilder.create().startBlock(artefact).endBlock().build();
				return plan;
			}

			@Override
			public Plan clonePlan(Plan plan) {
				Plan newPlan = plan;
				newPlan.setId(new ObjectId());
				newPlan.setCustomFields(null);
				newPlan.setVisible(true);
				return newPlan;
			}
		});
		context.put(PlanTypeRegistry.class, planTypeRegistry);
		context.getServiceRegistrationCallback().registerService(PlanServices.class);
		
		context.get(CollectionRegistry.class).register("plans", new PlanCollection(context.getMongoClientSession().getMongoDatabase()));
	}
	
	@Override
	public void initializeData(GlobalContext context) throws Exception {
		createScreenInputDefinitionsIfNecessary(context);
	}

	protected void createScreenInputDefinitionsIfNecessary(GlobalContext context) {
		// Plan table
		ScreenInputAccessor screenInputAccessor = context.get(ScreenInputAccessor.class);
		List<ScreenInput> screenInputsByScreenId = screenInputAccessor.getScreenInputsByScreenId("planTable");
		Input nameInput = new Input(InputType.TEXT, "attributes.name", "Name", null, null);
		nameInput.setValueHtmlTemplate("<entity-icon entity=\"stBean\" entity-name=\"'plans'\"/> <plan-link plan-id=\"stBean.id\" description=\"stBean.attributes.name\" st-options=\"stOptions\"/>");
		AtomicBoolean inputExists = new AtomicBoolean(false);
		// Force content of input 'attributes.name'
		screenInputsByScreenId.forEach(i->{
			Input input = i.getInput();
			if(input.getId().equals("attributes.name")) {
				i.setInput(nameInput);
				screenInputAccessor.save(i);
				inputExists.set(true);
			}
		});
		// Create it if not existing
		if(!inputExists.get()) {
			screenInputAccessor.save(new ScreenInput(0, "planTable", nameInput));
		}
	}
}
