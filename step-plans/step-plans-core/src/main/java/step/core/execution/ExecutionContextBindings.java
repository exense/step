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

import java.util.HashMap;
import java.util.Map;

public class ExecutionContextBindings {
	
	public static final String BINDING_RESOURCE_MANAGER = "resourceManager";

	public static Map<String, Object> get(ExecutionContext context) {
		Map<String, Object> bindings = new HashMap<String, Object>();
		bindings.putAll(context.getVariablesManager().getAllVariables());
		bindings.put("context", context);
		bindings.put("variables", context.getVariablesManager());
		bindings.put(BINDING_RESOURCE_MANAGER, context.getResourceManager());
		return bindings;
	}
	
}

