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
package step.plugins.java.handler;

import javax.json.JsonObject;

import step.functions.handler.JsonBasedFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.plugins.js223.handler.ScriptHandler;

public class GeneralScriptHandler extends JsonBasedFunctionHandler {

	@Override
	public Output<JsonObject> handle(Input<JsonObject> input) throws Exception {
		// Using the forked to branch in order no to have the ClassLoader of java-plugin-handler.jar as parent.
		// the project java-plugin-handler.jar has many dependencies that might conflict with the dependencies of the 
		// keyword. One of these dependencies is guava for example.
		pushLocalApplicationContext(FORKED_BRANCH, getCurrentContext().getClassLoader(), "step-functions-plugins-java-keyword-handler.jar");
		
		pushRemoteApplicationContext(FORKED_BRANCH, ScriptHandler.PLUGIN_LIBRARIES_FILE, input.getProperties());
		
		pushRemoteApplicationContext(FORKED_BRANCH, ScriptHandler.LIBRARIES_FILE, input.getProperties());
		
		String scriptLanguage = input.getProperties().get(ScriptHandler.SCRIPT_LANGUAGE);
		Class<?> handlerClass = scriptLanguage.equals("java")?JavaJarHandler.class:ScriptHandler.class;
		return delegate(handlerClass.getName(), input);		
	}

}
