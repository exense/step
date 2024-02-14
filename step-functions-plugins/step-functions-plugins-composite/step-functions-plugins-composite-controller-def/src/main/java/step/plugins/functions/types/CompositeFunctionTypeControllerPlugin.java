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
package step.plugins.functions.types;

import step.core.GlobalContext;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plans.PlanPlugin;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.functions.Function;
import step.functions.editors.FunctionEditor;
import step.functions.editors.FunctionEditorRegistry;
import step.functions.plugin.FunctionControllerPlugin;
import step.functions.type.FunctionTypeRegistry;
import step.plugins.functions.types.composite.services.CompositeFunctionServices;

@Plugin(dependencies= {FunctionControllerPlugin.class, PlanPlugin.class, ObjectHookControllerPlugin.class})
public class CompositeFunctionTypeControllerPlugin extends AbstractControllerPlugin {

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		super.serverStart(context);

		context.getServiceRegistrationCallback().registerService(CompositeFunctionServices.class);

		FunctionTypeRegistry functionTypeRegistry = context.require(FunctionTypeRegistry.class);
		ObjectHookRegistry objectHookRegistry = context.get(ObjectHookRegistry.class);
		functionTypeRegistry.registerFunctionType(new CompositeFunctionType(objectHookRegistry));

		context.get(FunctionEditorRegistry.class).register(new FunctionEditor() {
			@Override
			public String getEditorPath(Function function) {
				return "/root/composites/editor/"+ function.getId();
			}

			@Override
			public boolean isValidForFunction(Function function) {
				return function instanceof CompositeFunction;
			}
		});
	}
}
