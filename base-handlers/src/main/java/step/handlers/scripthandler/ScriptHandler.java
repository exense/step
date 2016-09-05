package step.handlers.scripthandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
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

	protected ScriptEngineManager manager = new ScriptEngineManager();

	protected boolean alwaysThrowExceptions;
		
	public ScriptHandler() {
		this(false);
	}

	public ScriptHandler(boolean alwaysThrowExceptions) {
		super();
		this.alwaysThrowExceptions = alwaysThrowExceptions;
	}
	
	public static final String SCRIPT_DIR = "scripthandler.script.dir";

	public static final String SCRIPT_ENGINE = "scripthandler.script.engine";
	
	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {        
        Map<String, String> properties = buildPropertyMap(token, message);
        
        String engineName = getScriptEngine(properties);
        ScriptEngine engine = loadScriptEngine(engineName);	
                
        String scriptDirectory = getScriptDirectory(properties);
        File scriptFile = searchScriptFile(message, scriptDirectory);        

        Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(scriptFile), Charset.forName("UTF-8")));     
        OutputMessageBuilder outputBuilder = new OutputMessageBuilder();
        Bindings binding = createBindings(token, message, outputBuilder, properties);     
        try {
        	engine.eval(reader, binding);
        } finally {
        	if(reader!=null) {
        		reader.close();
        	}
        }
        
        return outputBuilder.build();
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
