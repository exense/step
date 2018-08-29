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

import static step.planbuilder.FunctionPlanBuilder.keyword;
import static step.planbuilder.FunctionPlanBuilder.keywordWithKeyValues;
import static step.planbuilder.FunctionPlanBuilder.session;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.artefacts.CallFunction;
import step.artefacts.TestCase;
import step.core.GlobalContext;
import step.core.access.User;
import step.core.access.UserAccessorImpl;
import step.core.artefacts.ArtefactAccessor;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.LocalPlanRepository;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.functions.Function;
import step.functions.accessor.FunctionAccessorImpl;
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
			
			logger.info("Setting up demo plans...");
			setupDemo(context);
			//setupExecuteProcessFunction(context);
			createSeleniumDemoPlan(context.getArtefactAccessor(), "Chrome");
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
	
//	private void setupExecuteProcessFunction(GlobalContext context) {		
//		Function executeProcessFunction = createFunction("ExecuteProcess", "class:step.handlers.processhandler.ProcessHandler");
//		
//		MongoCollection functionCollection = MongoDBAccessorHelper.getCollection(context.getMongoClient(), "functions");				
//		FunctionRepositoryImpl functionRepository = new FunctionRepositoryImpl(functionCollection);
//		functionRepository.addFunction(executeProcessFunction);
//	}

	private void setupDemo(GlobalContext context) {
		FunctionCRUDAccessor functionRepository = new FunctionAccessorImpl(context.getMongoClientSession());
		
		JsonObject schema = null;
		if(context.getConfiguration().getPropertyAsBoolean("enforceschemas", false)){
			StringReader sr = new StringReader("{\"properties\":{\"label\":{\"type\":\"string\"}},\"required\":[\"label\"]}");
			schema = Json.createReader(sr).readObject();
			sr.close();
		}
		Function javaFunction = addScriptFunction(functionRepository, "Demo_Keyword_Java", "java", "../data/scripts/demo-java-keyword.jar", schema);
		
		Function javascriptFunction = addScriptFunction(functionRepository, "Demo_Keyword_Javascript", "javascript", "../data/scripts/Demo_Keyword_Javascript.js");
		
		Function openChrome = addSeleniumFunction(functionRepository, "Open_Chrome", "java", "../data/scripts/demo-selenium-keyword.jar" );
		
		Function googleSearch = addSeleniumFunction(functionRepository, "Google_Search", "java", "../data/scripts/demo-selenium-keyword.jar" );
		
		Function googleSearchMock = addSeleniumFunction(functionRepository, "Google_Search_Mock", "javascript", "../data/scripts/Google_Search_Mock.js" );
		
		Function jmeterDemoFunction = addJMeterFunction(functionRepository, "Demo_Keyword_JMeter", "../data/scripts/Demo_JMeter.jmx");
	}
	
	private CallFunction createCallFunctionById(String functionId, String args) {
		CallFunction call1 = new CallFunction();
		call1.setFunctionId(functionId);
		call1.setArgument(new DynamicValue<String>(args));
		return call1;
	}
	
	private void createSeleniumDemoPlan(ArtefactAccessor artefacts, String browser) {
		Map<String, String> tcAttributes = new HashMap<>();
		TestCase testCase = new TestCase();
		testCase.setRoot(true);
		
		tcAttributes.put("name", "Demo_Selenium_" + browser);
		testCase.setAttributes(tcAttributes);
				
		Plan plan = PlanBuilder.create()
				.startBlock(testCase)
					.startBlock(session())
						.add(keyword("Open_Chrome"))
						.add(keywordWithKeyValues("Google_Search", "search", "denkbar"))
						.endBlock()
				.endBlock()
				.build();
		LocalPlanRepository repo = new LocalPlanRepository(artefacts);
		repo.save(plan);
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
