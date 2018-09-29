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
package step.plugins.js223.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.filemanager.FileManagerClient.FileVersion;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class ScriptHandler extends AbstractMessageHandler {

	public static final String SCRIPT_LANGUAGE = "$scriptlanguage";

	public static final String SCRIPT_FILE = "$function.library.file";
	
	public static final String LIBRARIES_FILE = "$libraries.file";
	
	public static final String ERROR_HANDLER_FILE = "$errorhandler.file";
	
	public static final Map<String, String> scriptLangugaeMap = new ConcurrentHashMap<>();
	
	protected ScriptEngineManager manager;
		
	public ScriptHandler() {
		scriptLangugaeMap.put("groovy", "groovy");
		scriptLangugaeMap.put("javascript", "nashorn");
	}
	
	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		return agentTokenServices.getApplicationContextBuilder().runInContext(()->{
			Map<String, String> properties = buildPropertyMap(token, message);
			
			File scriptFile = retrieveFileVersion(ScriptHandler.SCRIPT_FILE, properties).getFile();
			
			String scriptLanguage = properties.get(SCRIPT_LANGUAGE);        
			String engineName = scriptLangugaeMap.get(scriptLanguage);
			ScriptEngine engine = loadScriptEngine(engineName);	      
			
			OutputMessageBuilder outputBuilder = new OutputMessageBuilder();
			Bindings binding = createBindings(token, message, outputBuilder, properties);     
			
			try {
				executeScript(scriptFile, binding, engine);        	
			} catch(Exception e) {        	
				boolean throwException = executeErrorHandlerScript(token, properties, engine, binding, outputBuilder, e);
				if(throwException) {
					outputBuilder.setError("Error while running script "+scriptFile.getName() + ": " + e.getMessage(), e);
				}
			}
			
			return outputBuilder.build();			
		});
	}

	private boolean executeErrorHandlerScript(AgentTokenWrapper token, Map<String, String> properties, ScriptEngine engine, Bindings binding, OutputMessageBuilder outputBuilder, Exception exception)
			throws FileNotFoundException, Exception, IOException {
		FileVersion errorScriptFileVersion = retrieveFileVersion(ScriptHandler.ERROR_HANDLER_FILE, properties);
		if(errorScriptFileVersion!=null) {
			File errorScriptFile = retrieveFileVersion(ScriptHandler.ERROR_HANDLER_FILE, properties).getFile();
			binding.put("exception", exception);
			try {
				executeScript(errorScriptFile, binding, engine);				
			} catch(Exception e) {
				outputBuilder.setError("Error while running error handler script: "+errorScriptFile.getName() + ". "+e.getMessage(), e);
			}
			return false;
		} else {
			return true;
		}
	}

	private void executeScript(File scriptFile, Bindings binding, ScriptEngine engine)
			throws FileNotFoundException, Exception, IOException {
		Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(scriptFile), Charset.forName("UTF-8")));     
        try {
        	engine.eval(reader, binding);
        } finally {
        	if(reader!=null) {
        		reader.close();
        	}
        }
	}

	private Bindings createBindings(AgentTokenWrapper token, InputMessage message, OutputMessageBuilder outputBuilder,
			Map<String, String> properties) {
		Bindings binding = new SimpleBindings();
        binding.put("input", message.getArgument());
        binding.put("inputJson", message.getArgument().toString());
        
        binding.put("output", outputBuilder);
        binding.put("context", outputBuilder);
        
        binding.put("properties", properties);
        binding.put("session", token.getTokenReservationSession());
        binding.put("tokenSession", token.getSession());
		return binding;
	}

	private ScriptEngine loadScriptEngine(String engineName) {
		manager = new ScriptEngineManager(Thread.currentThread().getContextClassLoader());
		ScriptEngine engine = manager.getEngineByName(engineName);
		if(engine==null) {
			throw new RuntimeException("Unable to find script engine with name '"+engineName+"'");
		}
		return engine;
	}
}
