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
package step.plugins.java;

import ch.exense.commons.app.Configuration;
import step.core.AbstractStepContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.functions.type.SetupFunctionException;

import java.util.Map;

public class GeneralScriptFunctionType extends AbstractScriptFunctionType<GeneralScriptFunction> {
		
	public GeneralScriptFunctionType(Configuration configuration) {
		super(configuration);
	}

	@Override
	public void setupFunction(GeneralScriptFunction function) throws SetupFunctionException {
		String language = getScriptLanguage(function);
		if(language.equals("java")) {
			// No specific setup for java at the moment
		} else {
			String template = null;
			if(language.equals("javascript")) {
				template = "custom_script.js";
			} else if(language.equals("groovy")) {
				template = "custom_script.groovy";
			}			
			setupScriptFile(function, template);
		}
	}

	@Override
	public GeneralScriptFunction newFunction() {
		GeneralScriptFunction function = new GeneralScriptFunction();
		function.getScriptLanguage().setValue("java");
		return function;
	}

	@Override
	public GeneralScriptFunction newFunction(Map<String, String> configuration) {
		GeneralScriptFunction function = this.newFunction();
		function.addAttribute(AbstractOrganizableObject.NAME, configuration.get("name"));
		function.setScriptFile(new DynamicValue<>(configuration.get("scriptFile")));
		return function;
	}

	@Override
	public Map<String, String> getHandlerProperties(GeneralScriptFunction function, AbstractStepContext context) {
		String language = getScriptLanguage(function);
		if (language != null) {
			String errorHandler = configuration.getProperty("plugins." + language + ".errorhandler", null);
			if (errorHandler != null) {
				function.setErrorHandlerFile(new DynamicValue<>(errorHandler));
			}
		}
		return super.getHandlerProperties(function, context);
	}
}
