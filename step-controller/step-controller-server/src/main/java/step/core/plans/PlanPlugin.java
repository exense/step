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
import step.plugins.table.settings.*;

import java.util.List;
import java.util.Optional;

@Plugin(dependencies= {ScreenTemplatePlugin.class, TableSettingsPlugin.class})
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
		createScreenInputAndTableDefinitionsIfNecessary(context);
	}

	protected void createScreenInputAndTableDefinitionsIfNecessary(GlobalContext context) {
		// Init Plan model and Table settings
		ScreenInputAccessor screenInputAccessor = context.get(ScreenInputAccessor.class);
		List<ScreenInput> screenInputsByScreenId = screenInputAccessor.getScreenInputsByScreenId(ScreenTemplatePlugin.PLAN_SCREEN_ID);
		// Search name input
		Optional<ScreenInput> nameInputOrig = screenInputsByScreenId.stream().filter(i -> i.getInput().getId().equals("attributes.name")).findFirst();
		// Create it if not existing
		if(nameInputOrig.isEmpty()) {
			Input nameInput = new Input(InputType.TEXT, "attributes.name", "Name", null, null);
			nameInput.setCustomUIComponents(List.of("planLink"));
			ScreenInput savedInput = screenInputAccessor.save(new ScreenInput(0, ScreenTemplatePlugin.PLAN_SCREEN_ID, nameInput, true));
			createTableSettingsIfNecessary(context, savedInput);
		} else {
			createTableSettingsIfNecessary(context, nameInputOrig.get());
		}
	}

	private void createTableSettingsIfNecessary(GlobalContext context, ScreenInput nameInput) {
		TableSettingsAccessor tableSettingsAccessor = context.get(TableSettingsAccessor.class);
		if (tableSettingsAccessor.findSystemTableSettings(EntityManager.plans).isEmpty()) {
			TableSettings setting = TableSettingsBuilder.builder().withSettingId(EntityManager.plans)
					.addColumn("bulkSelection", true)
					.addColumn("attributes.project", true)
					.addColumn("entityLock", true)
					.addColumn("attributes.name", true, nameInput)
					.addColumn("type", true)
					.addColumn("automationPackage", true)
					.addColumn("actions", true)
					.build();
			tableSettingsAccessor.save(setting);
		}
	}


}
