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
package step.core.execution;

import step.automation.packages.AutomationPackageEntity;

import java.util.HashMap;
import java.util.Map;

public class ExecutionContextBindings {
	
	public static final String BINDING_RESOURCE_MANAGER = "resourceManager";
	public static final String BINDING_AP = "automationPackageId";
	public static final String CONFIG_KEY_ALLOW_SENSITIVE_GROOVY_BINDINGS = "tec.expressions.allow.sensitive.groovy.bindings";


	// Static lazy-loaded configuration value
	private static volatile Boolean allowSensitiveGroovyBindings;

	public static Map<String, Object> get(ExecutionContext context) {
		// Thread-safe lazy initialization of static configuration value
		if (allowSensitiveGroovyBindings == null) {
			synchronized (ExecutionContextBindings.class) {
				if (allowSensitiveGroovyBindings == null) {
					allowSensitiveGroovyBindings = context.getConfiguration()
							.getPropertyAsBoolean(CONFIG_KEY_ALLOW_SENSITIVE_GROOVY_BINDINGS, false);
				}
			}
		}
		Map<String, Object> bindings = new HashMap<String, Object>();
		if(allowSensitiveGroovyBindings) {
			bindings.put("context", context);
			bindings.put("variables", context.getVariablesManager());
			bindings.put(BINDING_RESOURCE_MANAGER, context.getResourceManager());
		}
		//Add all variables as binding
		bindings.putAll(context.getVariablesManager().getAllVariables());
		//Add plan, useful for activation expression and not sensitive
		bindings.put("plan", context.getPlan());
		//Add the current AP id in context, used for the function and plan selection criteria (priority to keywords and plans from same package)
		if (context.getPlan() != null && context.getPlan().getCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID) != null) {
			bindings.put(BINDING_AP, context.getPlan().getCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID));
		}
		return bindings;
	}
	
}

