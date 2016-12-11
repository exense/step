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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jongo.MongoCollection;

import step.artefacts.CallFunction;
import step.artefacts.Check;
import step.artefacts.Return;
import step.artefacts.TestCase;
import step.core.GlobalContext;
import step.core.access.User;
import step.core.access.UserAccessor;
import step.core.accessors.MongoDBAccessorHelper;
import step.core.artefacts.ArtefactAccessor;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.functions.Function;
import step.plugins.adaptergrid.FunctionRepositoryImpl;

@Plugin
public class InitializationPlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		MongoCollection controllerLogs = MongoDBAccessorHelper.getCollection(context.getMongoClient(), "controllerlogs");
		
		long runCounts = controllerLogs.count();
		
		if(runCounts==0) {
			// First start
			setupUsers(context);
			setupDemo(context);
			setupExecuteProcessFunction(context);
		}
		
		insertLogEntry(controllerLogs);
		
		super.executionControllerStart(context);
	}

	private void setupUsers(GlobalContext context) {
		User user = new User();
		user.setUsername("admin");
		user.setRole("admin");
		user.setPassword(UserAccessor.encryptPwd("init"));
		context.getUserAccessor().save(user);
	}

	private void insertLogEntry(MongoCollection controllerLogs) {
		ControllerLog logEntry = new ControllerLog();
		logEntry.setStart(new Date());
		controllerLogs.insert(logEntry);
	}
	
	private void setupExecuteProcessFunction(GlobalContext context) {		
		Function executeProcessFunction = createFunction("ExecuteProcess", "class:step.handlers.processhandler.ProcessHandler");
		
		MongoCollection functionCollection = MongoDBAccessorHelper.getCollection(context.getMongoClient(), "functions");				
		FunctionRepositoryImpl functionRepository = new FunctionRepositoryImpl(functionCollection);
		functionRepository.addFunction(executeProcessFunction);
	}

	private void setupDemo(GlobalContext context) {
		MongoCollection functionCollection = MongoDBAccessorHelper.getCollection(context.getMongoClient(), "functions");				
		FunctionRepositoryImpl functionRepository = new FunctionRepositoryImpl(functionCollection);
		
		addFunction(functionRepository, "Demo_Echo");
		addFunction(functionRepository, "Javascript_HttpGet");
		addFunction(functionRepository, "Grinder_HttpGet", "classuri:../ext/lib/grinder|class:step.handlers.scripthandler.ScriptHandler");
		
		addFunction(functionRepository, "Selenium_StartChrome");
		addFunction(functionRepository, "Selenium_StartFirefox");
		addFunction(functionRepository, "Selenium_Navigate");
		
		ArtefactAccessor artefacts = context.getArtefactAccessor();
		
		createDemoPlan(artefacts,"Demo_TestCase_Echo","Demo_Echo","{\"arg1\":\"val1\"}","output.getString(\"output1\")==\"val1\"");
		createDemoPlan(artefacts,"Demo_TestCase_Javascript_HttpGet","Javascript_HttpGet","{\"url\":\"http://www.denkbar.io\"}","output.getInt(\"statusCode\")==200");
		createDemoPlan(artefacts,"Demo_TestCase_Grinder_HttpGet","Grinder_HttpGet","{\"url\":\"http://www.denkbar.io\"}",null);

		createSeleniumDemoPlan(artefacts);
	}

	private void createDemoPlan(ArtefactAccessor artefacts, String planName, String functionName, String args, String check) {
		Map<String, String> tcAttributes = new HashMap<>();
		TestCase testCase = new TestCase();
		testCase.setRoot(true);
		
		tcAttributes.put("name", planName);
		testCase.setAttributes(tcAttributes);
		
		CallFunction call1 = new CallFunction();
		call1.setFunction("{\"name\":\""+functionName+"\"}");
		call1.setArgument(args);
		call1.setToken("{\"route\":\"remote\"}");

		if(check!=null) {
			Check check1 = new Check();
			check1.setExpression(check);
			artefacts.save(check1);
			call1.addChild(check1.getId());
		}
		
		artefacts.save(call1);
		
		testCase.addChild(call1.getId());
		
		testCase.setRoot(true);
		artefacts.save(testCase);
	}
	
	private void createSeleniumDemoPlan(ArtefactAccessor artefacts) {
		Map<String, String> tcAttributes = new HashMap<>();
		TestCase testCase = new TestCase();
		testCase.setRoot(true);
		
		tcAttributes.put("name", "Demo_Selenium");
		testCase.setAttributes(tcAttributes);
		
		CallFunction call1 = new CallFunction();
		call1.setFunction("{\"name\":\"Selenium_StartFirefox\"}");
		call1.setArgument("{}");
		call1.setToken("{\"route\":\"remote\"}");
		artefacts.save(call1);
		
		CallFunction call2 = new CallFunction();
		call2.setFunction("{\"name\":\"Selenium_Navigate\"}");
		call2.setArgument("{\"url\":\"http://denkbar.io\"}");
		call2.setToken("{\"route\":\"remote\"}");
		artefacts.save(call2);
		
		testCase.addChild(call1.getId());
		testCase.addChild(call2.getId());
		
		testCase.setRoot(true);
		artefacts.save(testCase);
	}
	
	private void addFunction(FunctionRepositoryImpl functionRepository, String name) {
		addFunction(functionRepository, name, "class:step.handlers.scripthandler.ScriptHandler");
	}

	
	private void addFunction(FunctionRepositoryImpl functionRepository, String name, String handlerChain) {
		Function demoFunction = createFunction(name, handlerChain);
		functionRepository.addFunction(demoFunction);
	}

	private Function createFunction(String name, String handlerChain) {
		Function demoFunction = new Function();
		
		Map<String, String> kwAttributes = new HashMap<>();
		kwAttributes.put("name", name);
		
		demoFunction.setAttributes(kwAttributes);
		demoFunction.setHandlerChain(handlerChain);
		return demoFunction;
	}

	
}
