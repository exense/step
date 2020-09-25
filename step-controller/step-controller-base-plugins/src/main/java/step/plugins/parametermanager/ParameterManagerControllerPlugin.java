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
package step.plugins.parametermanager;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.parameter.Parameter;
import step.core.GlobalContext;
import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.CRUDAccessor;
import step.core.accessors.collections.CollectionRegistry;
import step.core.entities.Entity;
import step.core.imports.GenericDBImporter;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.ExecutionEnginePlugin;
import step.parameter.ParameterManager;
import step.plugins.screentemplating.Input;
import step.plugins.screentemplating.InputType;
import step.plugins.screentemplating.ScreenInput;
import step.plugins.screentemplating.ScreenInputAccessor;
import step.plugins.screentemplating.ScreenTemplatePlugin;

@Plugin(dependencies= {ScreenTemplatePlugin.class})
public class ParameterManagerControllerPlugin extends AbstractControllerPlugin {
	
	private static final String entityName = "parameters";

	public static Logger logger = LoggerFactory.getLogger(ParameterManagerControllerPlugin.class);
		
	protected ParameterManager parameterManager;
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		AbstractCRUDAccessor<Parameter> parameterAccessor = new AbstractCRUDAccessor<>(context.getMongoClientSession(), "parameters", Parameter.class);
		context.put("ParameterAccessor", parameterAccessor);
		
		context.get(CollectionRegistry.class).register("parameters", new ParameterCollection(context.getMongoClientSession().getMongoDatabase()));
		
		ParameterManager parameterManager = new ParameterManager(parameterAccessor, context.getConfiguration());
		context.put(ParameterManager.class, parameterManager);
		this.parameterManager = parameterManager;
		
		context.getEntityManager().register(new Entity<Parameter, CRUDAccessor<Parameter>> (
				ParameterManagerControllerPlugin.entityName, 
				parameterAccessor,
				Parameter.class,
				new GenericDBImporter<Parameter, CRUDAccessor<Parameter>>(context)));
		
		context.getServiceRegistrationCallback().registerService(ParameterServices.class);
	}

	@Override
	public void initializeData(GlobalContext context) throws Exception {
		createScreenInputDefinitionsIfNecessary(context);
	}

	@Override
	public ExecutionEnginePlugin getExecutionEnginePlugin() {
		return new ParameterManagerPlugin(parameterManager);
	}

	private static final String PARAMETER_DIALOG = "parameterDialog";
	private static final String PARAMETER_TABLE = "parameterTable";

	private void createScreenInputDefinitionsIfNecessary(GlobalContext context) {
		// Parameter table
		ScreenInputAccessor screenInputAccessor = context.get(ScreenInputAccessor.class);
		List<ScreenInput> parameterTable = screenInputAccessor.getScreenInputsByScreenId(PARAMETER_TABLE);
		Input keyInput = new Input(InputType.TEXT, "key", "Key", "Keys containing 'pwd' or 'password' will be automatically protected", null);
		keyInput.setValueHtmlTemplate("<entity-icon entity=\"stBean\" entity-name=\"'parameter'\"/> <parameter-key parameter=\"stBean\" st-options=\"stOptions\" />");
		if(parameterTable.isEmpty()) {
			screenInputAccessor.save(new ScreenInput(0, PARAMETER_TABLE, keyInput));
			screenInputAccessor.save(new ScreenInput(1, PARAMETER_TABLE, new Input(InputType.TEXT, "value", "Value", null, null)));
			screenInputAccessor.save(new ScreenInput(2, PARAMETER_TABLE, new Input(InputType.TEXT, "activationExpression.script", "Activation script", null, null)));
			screenInputAccessor.save(new ScreenInput(3, PARAMETER_TABLE, new Input(InputType.TEXT, "priority", "	Priority", null, null)));
		}
		
		// Ensure the key input is always up to date
		parameterTable.forEach(i->{
			Input input = i.getInput();
			if(input.getId().equals("key")) {
				i.setInput(keyInput);
				screenInputAccessor.save(i);
			}
		});
		
		// Edit parameter dialog
		if(screenInputAccessor.getScreenInputsByScreenId(PARAMETER_DIALOG).isEmpty()) {
			Input input = new Input(InputType.TEXT, "key", "Key", "Keys containing 'pwd' or 'password' will be automatically protected", null);
			screenInputAccessor.save(new ScreenInput(0, PARAMETER_DIALOG, input));
			screenInputAccessor.save(new ScreenInput(1, PARAMETER_DIALOG, new Input(InputType.TEXT, "value", "Value", null, null)));
			screenInputAccessor.save(new ScreenInput(2, PARAMETER_DIALOG, new Input(InputType.TEXT, "description", "Description", null, null)));
			screenInputAccessor.save(new ScreenInput(3, PARAMETER_DIALOG, new Input(InputType.TEXT, "activationExpression.script", "Activation script", null, null)));
			screenInputAccessor.save(new ScreenInput(4, PARAMETER_DIALOG, new Input(InputType.TEXT, "priority", "	Priority", null, null)));
		}
	}
}
