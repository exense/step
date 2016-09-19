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

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jongo.MongoCollection;

import step.artefacts.CallFunction;
import step.artefacts.Check;
import step.artefacts.Set;
import step.artefacts.TestCase;
import step.core.GlobalContext;
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
			setupDemo(context);
			
			
		}
		
		insertLogEntry(controllerLogs);
		
		super.executionControllerStart(context);
	}

	private void insertLogEntry(MongoCollection controllerLogs) {
		ControllerLog logEntry = new ControllerLog();
		logEntry.setStart(new Date());
		controllerLogs.insert(logEntry);
	}

	private void setupDemo(GlobalContext context) {
		MongoCollection functionCollection = MongoDBAccessorHelper.getCollection(context.getMongoClient(), "functions");				
		FunctionRepositoryImpl functionRepository = new FunctionRepositoryImpl(functionCollection);
		
		addFunction(functionRepository, "Demo_Echo");
		addFunction(functionRepository, "Demo_HTTPGet");
		
		ArtefactAccessor artefacts = context.getArtefactAccessor();
		
		createDemoPlan(artefacts,"Demo_TestCase_Echo","Demo_Echo","{\"arg1\":\"val1\"}","output.getString(\"output1\")==\"val1\"");
		createDemoPlan(artefacts,"Demo_TestCase_HTTPGet","Demo_HTTPGet","{\"url\":\"http://denkbar.io\"}","output.getInt(\"statusCode\")==200");
	}

	private void createDemoPlan(ArtefactAccessor artefacts, String planName, String functionName, String args, String check) {
		Map<String, String> tcAttributes = new HashMap<>();
		TestCase testCase = new TestCase();
		testCase.setRoot(true);
		
		tcAttributes.put("name", planName);
		testCase.setAttributes(tcAttributes);
		
		Set set1 = new Set();
		set1.setKey("scripthandler.script.dir");
		
		String scriptPath = getDemoScriptPath();
		
		set1.setExpression("'"+scriptPath+"'");
		artefacts.save(set1);
		
		
		Check check1 = new Check();
		check1.setExpression(check);
		artefacts.save(check1);
		
		CallFunction call1 = new CallFunction();
		call1.setFunction("{\"name\":\""+functionName+"\"}");
		call1.setArgument(args);
		call1.setToken("{\"route\":\"remote\"}");
		call1.addChild(check1.getId());
		artefacts.save(call1);
		
		
		testCase.addChild(set1.getId());
		testCase.addChild(call1.getId());
		
		testCase.setRoot(true);
		artefacts.save(testCase);
	}

	private String getDemoScriptPath() {
		String scriptPath = "/path/to/your/scripts";
		String currentDir = System.getProperty("user.dir");
		if(currentDir!=null) {
			File demoScripts = new File(currentDir+"/../data/scripts");
			if(demoScripts.exists()) {
				try {
					scriptPath = demoScripts.getCanonicalPath().replace("\\", "/");
				} catch (IOException e) {
					
				}
			}
		}
		return scriptPath;
	}

	private void addFunction(FunctionRepositoryImpl functionRepository, String name) {
		Function demoFunction = new Function();
		
		Map<String, String> kwAttributes = new HashMap<>();
		kwAttributes.put("name", name);
		
		demoFunction.setAttributes(kwAttributes);
		demoFunction.setHandlerChain("class:step.handlers.scripthandler.ScriptHandler");
		functionRepository.addFunction(demoFunction);
	}

	
}
