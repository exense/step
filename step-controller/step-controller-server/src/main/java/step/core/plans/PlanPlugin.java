/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.plans;

import org.bson.types.ObjectId;
import step.core.GlobalContext;
import step.core.artefacts.AbstractArtefact;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.entities.EntityManager;
import step.core.plans.builder.PlanBuilder;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.plugins.screentemplating.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Plugin(dependencies= {ScreenTemplatePlugin.class})
public class PlanPlugin extends AbstractControllerPlugin {

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		PlanTypeRegistry planTypeRegistry = new PlanTypeRegistry();
		planTypeRegistry.register(new PlanType<>() {

			@Override
			public Class<Plan> getPlanClass() {
				return Plan.class;
			}

			@Override
			public PlanCompiler<Plan> getPlanCompiler() {
				return plan -> plan;
			}

			@Override
			public Plan newPlan(String template) throws Exception {
				AbstractArtefact artefact = context.getArtefactHandlerRegistry().getArtefactTypeInstance(template);
				return PlanBuilder.create().startBlock(artefact).endBlock().build();
			}

			@Override
			public Plan clonePlan(Plan plan, boolean updateVisibility) {
				plan.setId(new ObjectId());
				if (updateVisibility) {
					plan.setCustomFields(null);
					plan.setVisible(true);
					if (plan.getRoot() != null) {
						// delete all custom attributes for all children to clean up attributes like "source" cloned from original plan
						plan.getRoot().deepCleanupAllCustomAttributes();
					}
				}
				return plan;
			}

			@Override
			public void onBeforeSave(Plan plan) {

			}
		});
		context.put(PlanTypeRegistry.class, planTypeRegistry);
		context.getServiceRegistrationCallback().registerService(PlanServices.class);
		Collection<Plan> collection = context.getCollectionFactory().getCollection(EntityManager.plans, Plan.class);
		context.get(TableRegistry.class).register(EntityManager.plans, new Table<>(collection, "plan-read", true)
				.withTableFiltersFactory(e-> Filters.equals("visible", true)));
	}
	
	@Override
	public void initializeData(GlobalContext context) throws Exception {
		createScreenInputDefinitionsIfNecessary(context);
	}

	protected void createScreenInputDefinitionsIfNecessary(GlobalContext context) {
		// Plan table
		ScreenInputAccessor screenInputAccessor = context.get(ScreenInputAccessor.class);
		List<ScreenInput> screenInputsByScreenId = screenInputAccessor.getScreenInputsByScreenId(ScreenTemplatePlugin.PLAN_TABLE);
		Input nameInput = new Input(InputType.TEXT, "attributes.name", "Name", null, null);
		nameInput.setCustomUIComponents(List.of("planLink"));
		AtomicBoolean inputExists = new AtomicBoolean(false);
		// Force content of input 'attributes.name'
		screenInputsByScreenId.forEach(i->{
			Input input = i.getInput();
			if(input.getId().equals("attributes.name")) {
				i.setInput(nameInput);
				i.setImmutable(true);
				screenInputAccessor.save(i);
				inputExists.set(true);
			}
		});
		// Create it if not existing
		if(!inputExists.get()) {
			screenInputAccessor.save(new ScreenInput(0, "planTable", nameInput, true));
		}
	}
}
