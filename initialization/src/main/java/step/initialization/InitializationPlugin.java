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

import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.Json;
import javax.json.JsonObject;

import org.bson.Document;
import org.jongo.MongoCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.Block;

import step.artefacts.CallFunction;
import step.artefacts.CallPlan;
import step.artefacts.Check;
import step.artefacts.TestCase;
import step.core.GlobalContext;
import step.core.access.User;
import step.core.access.UserAccessor;
import step.core.accessors.MongoDBAccessorHelper;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.dynamicbeans.DynamicValue;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.functions.Function;
import step.plugins.adaptergrid.FunctionRepositoryImpl;
import step.plugins.functions.types.GeneralScriptFunction;
import step.plugins.jmeter.JMeterFunction;
import step.plugins.selenium.SeleniumFunction;

@Plugin
public class InitializationPlugin extends AbstractPlugin {

	private static final Logger logger = LoggerFactory.getLogger(InitializationPlugin.class);
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		MongoCollection controllerLogs = MongoDBAccessorHelper.getCollection(context.getMongoClient(), "controllerlogs");
		
		long runCounts = controllerLogs.count();
		
		if(runCounts==0) {
			// First start
			setupUsers(context);
			setupDemo(context);
			//setupExecuteProcessFunction(context);
		}
		
		migrateCallFunction(context);
		
		// TODO do this only when migrating from 3.4.0 to 3.5.0 or higher
		renameArtefactType(context, "FunctionGroup", "Session");
		renameArtefactType(context, "CallFunction", "CallKeyword");
		
		setArtefactNameIfEmpty(context);
		
		insertLogEntry(controllerLogs);
		
		super.executionControllerStart(context);
	}

	private void setupUsers(GlobalContext context) {
		User user = new User();
		user.setUsername("admin");
		user.setRole("default");
		user.setPassword(UserAccessor.encryptPwd("init"));
		context.getUserAccessor().save(user);
	}
	
	// This function migrates the artefact of type 'CallFunction' that have the attribute 'function' declared as string instead of DynamicValue
	// TODO do this only when migrating from 3.4.0 to 3.5.0 or higher
	private void migrateCallFunction(GlobalContext context) {
		logger.info("Searching for artefacts of type 'CallFunction' to be migrated...");
		com.mongodb.client.MongoCollection<Document> artefacts = MongoDBAccessorHelper.getMongoCollection_(context.getMongoClient(), "artefacts");
		
		AtomicInteger i = new AtomicInteger();
		Document filterCallFunction = new Document("_class", "CallFunction");
		artefacts.find(filterCallFunction).forEach(new Block<Document>() {

			@Override
			public void apply(Document t) {
				if(t.containsKey("function")) {
					try {
						i.incrementAndGet();
						String function = t.getString("function");
						Document d = new Document();
						d.append("dynamic", false);
						d.append("value", function);
						t.replace("function", d);
						
						Document filter = new Document("_id", t.get("_id"));
						
						artefacts.replaceOne(filter, t);
						logger.info("Migrating "+function+" to "+d.toJson());
					} catch(ClassCastException e) {
						// ignore
					}
				}
			}
		});
		
		logger.info("Migrated "+i.get()+" artefacts of type 'CallFunction'");
	}

	private void renameArtefactType(GlobalContext context, String classFrom, String classTo) {
		logger.info("Searching for artefacts of type '"+classFrom+"' to be migrated...");
		com.mongodb.client.MongoCollection<Document> artefacts = MongoDBAccessorHelper.getMongoCollection_(context.getMongoClient(), "artefacts");
		
		AtomicInteger i = new AtomicInteger();
		Document filterCallFunction = new Document("_class", classFrom);
		artefacts.find(filterCallFunction).forEach(new Block<Document>() {

			@Override
			public void apply(Document t) {
				try {
					i.incrementAndGet();
					t.put("_class", classTo);
					
					Document filter = new Document("_id", t.get("_id"));
					
					artefacts.replaceOne(filter, t);
					logger.info("Migrating "+classFrom+ " to "+t.toJson());
				} catch(ClassCastException e) {
					// ignore
				}
			}
		});
		
		logger.info("Migrated "+i.get()+" artefacts of type '"+classFrom+"'");
	}
	
	
	
	// This function ensures that all the artefacts have their name saved properly in the attribute map. 
	// This will only be needed for the migration from 3.3.x or lower to 3.4.x or higher
	private void setArtefactNameIfEmpty(GlobalContext context) {
		MongoCollection functionCollection = MongoDBAccessorHelper.getCollection(context.getMongoClient(), "functions");				
		FunctionRepositoryImpl functionRepository = new FunctionRepositoryImpl(functionCollection);
		
		ArtefactAccessor a = context.getArtefactAccessor();
		
		AbstractArtefact artefact;
		Iterator<AbstractArtefact> it = a.getAll();
		while(it.hasNext()) {
			artefact = it.next();
			Map<String,String> attributes = artefact.getAttributes();
			if(attributes==null) {
				attributes = new HashMap<>();
				artefact.setAttributes(attributes);
			}
			
			if(!attributes.containsKey("name")) {
				String name = null;
				if(artefact instanceof CallFunction) {
					CallFunction calllFunction = (CallFunction) artefact;
					if(calllFunction.getFunctionId()!=null) {
						Function function = functionRepository.getFunctionById(calllFunction.getFunctionId());
						if(function!=null && function.getAttributes()!=null && function.getAttributes().containsKey(Function.NAME)) {
							name = function.getAttributes().get(Function.NAME);
						}						
					}
				} else if(artefact instanceof CallPlan) {
					CallPlan callPlan = (CallPlan) artefact;
					if(callPlan.getArtefactId()!=null) {
						AbstractArtefact calledArtefact = a.get(callPlan.getArtefactId());
						if(calledArtefact != null && calledArtefact.getAttributes()!=null && calledArtefact.getAttributes().containsKey("name")) {
							name = calledArtefact.getAttributes().get("name");
						}						
					}
				}
				if(name == null) {
					Artefact annotation = artefact.getClass().getAnnotation(Artefact.class);
					name =  annotation.name().length() > 0 ? annotation.name() : getClass().getSimpleName();
				}
				attributes.put("name", name);
				a.save(artefact);
			}
		}
	}
	
	private void insertLogEntry(MongoCollection controllerLogs) {
		ControllerLog logEntry = new ControllerLog();
		logEntry.setStart(new Date());
		controllerLogs.insert(logEntry);
	}
	
//	private void setupExecuteProcessFunction(GlobalContext context) {		
//		Function executeProcessFunction = createFunction("ExecuteProcess", "class:step.handlers.processhandler.ProcessHandler");
//		
//		MongoCollection functionCollection = MongoDBAccessorHelper.getCollection(context.getMongoClient(), "functions");				
//		FunctionRepositoryImpl functionRepository = new FunctionRepositoryImpl(functionCollection);
//		functionRepository.addFunction(executeProcessFunction);
//	}

	private void setupDemo(GlobalContext context) {
		MongoCollection functionCollection = MongoDBAccessorHelper.getCollection(context.getMongoClient(), "functions");				
		FunctionRepositoryImpl functionRepository = new FunctionRepositoryImpl(functionCollection);
		
		JsonObject schema = null;
		if(context.getConfiguration().getPropertyAsBoolean("enforceschemas", false)){
			StringReader sr = new StringReader("{\"properties\":{\"label\":{\"type\":\"string\"}},\"required\":[\"label\"]}");
			schema = Json.createReader(sr).readObject();
			sr.close();
		}
		Function javaFunction = addScriptFunction(functionRepository, "Demo_Keyword_Java", "java", "../data/scripts/demo-java-keyword/target/classes", schema);
		
		Function javascriptFunction = addScriptFunction(functionRepository, "Demo_Keyword_Javascript", "javascript", "../data/scripts/Demo_Keyword_Javascript.js");
		
		Function googleSearch = addSeleniumFunction(functionRepository, "Google_Search", "java", "../data/scripts/demo-selenium-keyword/target/classes" );
		Function googleSearchMock = addSeleniumFunction(functionRepository, "Google_Search_Mock", "javascript", "../data/scripts/Google_Search_Mock.js" );
		
		Function jmeterDemoFunction = addJMeterFunction(functionRepository, "Demo_Keyword_JMeter", "../data/scripts/Demo_JMeter.jmx");
	}
//
//	private void createDemoForEachPlan(ArtefactAccessor artefacts, String planName)  {
//		CallFunction call1 = createCallFunctionWithCheck(artefacts,"Javascript_HttpGet","{\"url\":\"[[dataPool.url]]\"}","output.getString(\"data\").contains(\"[[dataPool.check]]\")");
//		
//		ForEachBlock forEach = new ForEachBlock();
//		CSVDataPool conf = new CSVDataPool();
//		conf.setFile(new DynamicValue<String>("../data/testdata/demo.csv"));
//		forEach.setDataSource(conf);
//		forEach.setDataSourceType("csv");
//		forEach.addChild(call1.getId());
//		artefacts.save(forEach);
//
//		Map<String, String> tcAttributes = new HashMap<>();
//		TestCase testCase = new TestCase();
//		testCase.setRoot(true);
//		
//		tcAttributes.put("name", planName);
//		testCase.setAttributes(tcAttributes);
//		testCase.addChild(forEach.getId());
//		artefacts.save(testCase);
//	}
//	
//	private void createDemoPlan(ArtefactAccessor artefacts, String planName, String functionId, String args, String check) {
//		Map<String, String> tcAttributes = new HashMap<>();
//		TestCase testCase = new TestCase();
//		testCase.setRoot(true);
//		
//		tcAttributes.put("name", planName);
//		testCase.setAttributes(tcAttributes);
//		
//		CallFunction call1 = createCallFunctionByIdWithCheck(artefacts, functionId, args, check);
//		
//		testCase.addChild(call1.getId());
//		
//		testCase.setRoot(true);
//		artefacts.save(testCase);
//	}

	private CallFunction createCallFunctionByIdWithCheck(ArtefactAccessor artefacts, String functionId, String args,
			String check) {
		CallFunction call1 = createCallFunctionById(functionId, args);

		if(check!=null) {
			Check check1 = new Check();
			check1.setExpression(new DynamicValue<>(check, ""));
			artefacts.save(check1);
			call1.addChild(check1.getId());
		}
		
		artefacts.save(call1);
		return call1;
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
		
		CallFunction call1 = new CallFunction();
		call1.getFunction().setValue("{\"name\":\"Selenium_Start"+ browser +"\"}");
		call1.setArgument(new DynamicValue<String>("{}"));
		artefacts.save(call1);
		
		CallFunction call2 = new CallFunction();
		call2.getFunction().setValue("{\"name\":\"Selenium_Navigate\"}");
		call2.setArgument(new DynamicValue<String>("{\"url\":\"http://denkbar.io\"}"));
		artefacts.save(call2);
		
		testCase.addChild(call1.getId());
		testCase.addChild(call2.getId());
		
		testCase.setRoot(true);
		artefacts.save(testCase);
	}
	
	private Function addScriptFunction(FunctionRepositoryImpl functionRepository, String name, String scriptLanguage, String scriptFile) {
		return addScriptFunction(functionRepository, name, scriptLanguage, scriptFile, null);
	}
	private Function addScriptFunction(FunctionRepositoryImpl functionRepository, String name, String scriptLanguage, String scriptFile, JsonObject schema) {
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
		functionRepository.addFunction(function);
		return function;
	}
	
	private Function addSeleniumFunction(FunctionRepositoryImpl functionRepository, String name, String scriptLanguage, String scriptFile) {
		SeleniumFunction function = new SeleniumFunction();
		Map<String, String> kwAttributes = new HashMap<>();
		kwAttributes.put(Function.NAME, name);
		function.setAttributes(kwAttributes);
		function.getScriptLanguage().setValue(scriptLanguage);
		function.getScriptFile().setValue(scriptFile);
		function.setSeleniumVersion("2.x");
		functionRepository.addFunction(function);
		return function;
	}
	
	private Function addJMeterFunction(FunctionRepositoryImpl functionRepository, String name, String jmeterFile) {
		JMeterFunction function = new JMeterFunction();
		Map<String, String> kwAttributes = new HashMap<>();
		kwAttributes.put(Function.NAME, name);
		function.setAttributes(kwAttributes);
		function.getJmeterTestplan().setValue(jmeterFile);
		functionRepository.addFunction(function);
		return function;
	}
}
