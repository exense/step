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
		
		Function demoFunction = new Function();
		
		Map<String, String> kwAttributes = new HashMap<>();
		kwAttributes.put("name", "Demo_Keyword");
		
		demoFunction.setAttributes(kwAttributes);
		demoFunction.setHandlerChain("class:step.handlers.scripthandler.ScriptHandler");
		functionRepository.addFunction(demoFunction);
		
		ArtefactAccessor artefacts = context.getArtefactAccessor();
		
		Map<String, String> tcAttributes = new HashMap<>();
		TestCase testCase = new TestCase();
		testCase.setRoot(true);
		
		tcAttributes.put("name", "Demo_TestCase");
		testCase.setAttributes(tcAttributes);
		
		Set set1 = new Set();
		set1.setKey("scripthandler.script.dir");
		
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
		
		set1.setExpression("'"+scriptPath+"'");
		artefacts.save(set1);
		
		
		Check check1 = new Check();
		check1.setExpression("output.getString(\"output1\")==\"val1\"");
		artefacts.save(check1);
		
		CallFunction call1 = new CallFunction();
		call1.setFunction("{\"name\":\"Demo_Keyword\"}");
		call1.setArgument("{\"arg1\":\"val1\"}");
		call1.setToken("{\"route\":\"remote\"}");
		call1.addChild(check1.getId());
		artefacts.save(call1);
		
		
		testCase.addChild(set1.getId());
		testCase.addChild(call1.getId());
		
		testCase.setRoot(true);
		artefacts.save(testCase);
	}

	
}
