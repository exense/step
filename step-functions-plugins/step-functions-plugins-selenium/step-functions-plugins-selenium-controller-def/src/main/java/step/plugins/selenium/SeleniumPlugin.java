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
package step.plugins.selenium;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.plugins.WebPlugin;
import step.functions.plugin.FunctionControllerPlugin;
import step.functions.type.FunctionTypeRegistry;

@Plugin(dependencies= {FunctionControllerPlugin.class})
public class SeleniumPlugin extends AbstractControllerPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		registerWebapp(context,"/seleniumplugin/");
		
		FunctionTypeRegistry functionTypeRegistry = context.get(FunctionTypeRegistry.class);
		functionTypeRegistry.registerFunctionType(new SeleniumFunctionType(context.getConfiguration()));
		
		super.executionControllerStart(context);
	}
	
	@Override
	public WebPlugin getWebPlugin() {
		WebPlugin webPlugin = new WebPlugin();
		webPlugin.getAngularModules().add("seleniumPlugin");
		webPlugin.getScripts().add("seleniumplugin/js/selenium.js");
		return webPlugin;
	}

}
