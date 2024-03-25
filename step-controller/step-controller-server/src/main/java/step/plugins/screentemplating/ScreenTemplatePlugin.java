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
package step.plugins.screentemplating;

import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.entities.Entity;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Plugin
public class ScreenTemplatePlugin extends AbstractControllerPlugin {

	public static final String FUNCTION_TABLE = "functionTable";
	public static final String EXECUTION_PARAMETERS = "executionParameters";
	public static final String PLAN_TABLE = "planTable";

	protected ScreenTemplateManager screenTemplateManager;
	protected ScreenInputAccessor screenInputAccessor;
	
	@Override
	public void serverStart(GlobalContext context) {
		screenInputAccessor = new ScreenInputAccessorImpl(
				context.getCollectionFactory().getCollection("screenInputs", ScreenInput.class));
		screenTemplateManager = new ScreenTemplateManager(screenInputAccessor, context.getConfiguration());
		FunctionTableScreenInputs functionTableScreenInputs = new FunctionTableScreenInputs(screenTemplateManager);

		initializeScreenInputsIfNecessary();
		
		context.put(ScreenInputAccessor.class, screenInputAccessor);
		context.put(ScreenTemplateManager.class, screenTemplateManager);
		context.put(FunctionTableScreenInputs.class, functionTableScreenInputs);
		context.getServiceRegistrationCallback().registerService(ScreenTemplateService.class);
		
		context.getEntityManager().register(
				new Entity<>("screenInputs", screenInputAccessor, ScreenInput.class));
		
		Collection<ScreenInput> collectionDriver = context.getCollectionFactory().getCollection("screenInputs",
				ScreenInput.class);
		context.get(TableRegistry.class).register("screenInputs", new Table<>(collectionDriver, null, true));
	}

	private void initializeScreenInputsIfNecessary() {
		// Function table
		List<ScreenInput> screenInputsByScreenId = screenInputAccessor.getScreenInputsByScreenId(ScreenTemplatePlugin.FUNCTION_TABLE);
		Input nameInput = new Input(InputType.TEXT, "attributes.name", "Name", null, null);
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
			screenInputAccessor.save(new ScreenInput(0, FUNCTION_TABLE, nameInput, true));
		}

		//Execution parameters
		if(screenInputAccessor.getScreenInputsByScreenId(EXECUTION_PARAMETERS).isEmpty()) {
			screenInputAccessor.save(new ScreenInput(EXECUTION_PARAMETERS, new Input(InputType.DROPDOWN, "env", "Environment", Arrays.asList(new Option("TEST"),new Option("PROD")))));
		}
	}
}
