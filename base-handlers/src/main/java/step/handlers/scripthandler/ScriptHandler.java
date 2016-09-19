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
package step.handlers.scripthandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

import step.grid.agent.handler.MessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class ScriptHandler implements MessageHandler {

	public static final String SCRIPT_DIR = "scripthandler.script.dir";
	public static final String SCRIPT_ENGINE = "scripthandler.script.engine";
	public static final String ERROR_HANDLER_SCRIPT = "scripthandler.script.errorhandler";
	
	protected ScriptEngineManager manager = new ScriptEngineManager();
		
	public ScriptHandler() {

	}
	
	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {        
        Map<String, String> properties = buildPropertyMap(token, message);
        
        String engineName = getScriptEngine(properties);
        ScriptEngine engine = loadScriptEngine(engineName);	
                
        String scriptDirectory = getScriptDirectory(properties);
        File scriptFile = searchScriptFile(message, scriptDirectory);        

        OutputMessageBuilder outputBuilder = new OutputMessageBuilder();
        Bindings binding = createBindings(token, message, outputBuilder, properties);     

        try {
        	executeScript(scriptFile, binding, engine);        	
        } catch(Exception e) {        	
        	executeErrorHandlerScript(properties, engine, binding);
        	throw e;
        }
        
        return outputBuilder.build();
	}

	private void executeErrorHandlerScript(Map<String, String> properties, ScriptEngine engine, Bindings binding)
			throws FileNotFoundException, Exception, IOException {
		if(properties.containsKey(ERROR_HANDLER_SCRIPT)) {
			String errorScript = properties.get(ERROR_HANDLER_SCRIPT);
			executeScript(new File(errorScript), binding, engine);
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
        binding.put("output", outputBuilder);
        binding.put("properties", properties);
        binding.put("session", token.getSession());
		return binding;
	}

	private ScriptEngine loadScriptEngine(String engineName) {
		ScriptEngine engine = manager.getEngineByName(engineName);
		if(engine==null) {
			throw new RuntimeException("Unable to find script engine with name '"+engineName+"'");
		}
		return engine;
	}

	private String getScriptEngine(Map<String, String> properties) {
		String engineName;
        if(properties.containsKey(SCRIPT_ENGINE)) {
        	engineName = properties.get(SCRIPT_ENGINE);
        } else {
        	engineName = "nashorn";
        }
		return engineName;
	}

	private String getScriptDirectory(Map<String, String> properties) {
		String scriptDirectory;
        if(properties.containsKey(SCRIPT_DIR)) {
        	scriptDirectory = properties.get(SCRIPT_DIR);
        } else {
        	throw new RuntimeException("Property '"+SCRIPT_DIR+"' is undefined.");
        }
		return scriptDirectory;
	}

	private File searchScriptFile(InputMessage message, String scriptDirectory) {
		File directory = new File(scriptDirectory);
        File[] files = directory.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.getName().startsWith(message.getFunction());
			}
        });
        if(files.length==0) {
        	throw new RuntimeException("No script found for function '"+message.getFunction()+"'");
        } else if(files.length>1) {
        	throw new RuntimeException("More than one script found for function '"+message.getFunction()+"'");
        }
        
        File scriptFile = files[0];
		return scriptFile;
	}

	private Map<String, String> buildPropertyMap(AgentTokenWrapper token, InputMessage message) {
		Map<String, String> properties = new HashMap<>();
		if(message.getProperties()!=null) {
			properties.putAll(message.getProperties());
		}
		if(token.getProperties()!=null) {
			properties.putAll(token.getProperties());			
		}
		return properties;
	}

}
