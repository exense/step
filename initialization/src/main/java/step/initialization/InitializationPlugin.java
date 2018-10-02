/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.initialization;

import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.artefacts.CallFunction;
import step.core.GlobalContext;
import step.core.access.User;
import step.core.access.UserAccessorImpl;
import step.core.dynamicbeans.DynamicValue;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.functions.Function;
import step.functions.accessor.FunctionCRUDAccessor;
import step.plugins.java.GeneralScriptFunction;
import step.plugins.jmeter.JMeterFunction;
import step.plugins.selenium.SeleniumFunction;
import step.versionmanager.VersionManager;

@Plugin(prio=2)
public class InitializationPlugin extends AbstractPlugin {

	private static final Logger logger = LoggerFactory.getLogger(InitializationPlugin.class);
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		VersionManager versionManager = context.get(VersionManager.class);

		if(versionManager.getLatestControllerLog()==null) {
			// First start
			logger.info("Initializing Users...");
			setupUsers(context);
		}
						
		super.executionControllerStart(context);
	}

	private void setupUsers(GlobalContext context) {
		User user = new User();
		user.setUsername("admin");
		user.setRole("admin");
		user.setPassword(UserAccessorImpl.encryptPwd("init"));
		context.getUserAccessor().save(user);
	}
	
	private CallFunction createCallFunctionById(String functionId, String args) {
		CallFunction call1 = new CallFunction();
		call1.setFunctionId(functionId);
		call1.setArgument(new DynamicValue<String>(args));
		return call1;
	}

	private Function addScriptFunction(FunctionCRUDAccessor functionRepository, String name, String scriptLanguage, String scriptFile) {
		return addScriptFunction(functionRepository, name, scriptLanguage, scriptFile, null);
	}
	
	private Function addScriptFunction(FunctionCRUDAccessor functionRepository, String name, String scriptLanguage, String scriptFile, JsonObject schema) {
		GeneralScriptFunction function = new GeneralScriptFunction();
		Map<String, String> kwAttributes = new HashMap<>();
		kwAttributes.put(Function.NAME, name);
		function.setAttributes(kwAttributes);
		function.getScriptLanguage().setValue(scriptLanguage);
		function.getScriptFile().setValue(scriptFile);
		if(schema != null){
			function.setSchema(schema);
		}else{
			function.setSchema(Json.createObjectBuilder().build());
		}
		functionRepository.save(function);
		return function;
	}
	
	private Function addSeleniumFunction(FunctionCRUDAccessor functionRepository, String name, String scriptLanguage, String scriptFile) {
		SeleniumFunction function = new SeleniumFunction();
		Map<String, String> kwAttributes = new HashMap<>();
		kwAttributes.put(Function.NAME, name);
		function.setAttributes(kwAttributes);
		function.getScriptLanguage().setValue(scriptLanguage);
		function.getScriptFile().setValue(scriptFile);
		function.setSeleniumVersion("3.x");
		functionRepository.save(function);
		return function;
	}
	
	private Function addJMeterFunction(FunctionCRUDAccessor functionRepository, String name, String jmeterFile) {
		JMeterFunction function = new JMeterFunction();
		Map<String, String> kwAttributes = new HashMap<>();
		kwAttributes.put(Function.NAME, name);
		function.setAttributes(kwAttributes);
		function.getJmeterTestplan().setValue(jmeterFile);
		functionRepository.save(function);
		return function;
	}
}
