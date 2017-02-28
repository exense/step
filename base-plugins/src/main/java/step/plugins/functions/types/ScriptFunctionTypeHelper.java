package step.plugins.functions.types;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import step.commons.conf.Configuration;
import step.core.GlobalContext;
import step.functions.FunctionClient;
import step.functions.type.SetupFunctionException;
import step.handlers.scripthandler.ScriptHandler;
import step.plugins.adaptergrid.GridPlugin;

public class ScriptFunctionTypeHelper {

	public static final Map<String, String> fileExtensionMap = new ConcurrentHashMap<>();
	
	public static final Map<String, String> scriptEngineMap = new ConcurrentHashMap<>();
	
	{
		fileExtensionMap.put("groovy", "groovy");
		scriptEngineMap.put("groovy", "groovy");
		fileExtensionMap.put("python", "py");
		scriptEngineMap.put("python", "python");
		fileExtensionMap.put("javascript", "js");
		scriptEngineMap.put("javascript", "nashorn");
	}
	
	private final GlobalContext context;
	
	public ScriptFunctionTypeHelper(GlobalContext context) {
		super();
		this.context = context;
	}

	public Map<String, String> getHandlerProperties(ScriptFunction function) {
		File scriptFile = getScriptFile(function);
		
		FunctionClient functionClient = (FunctionClient) context.get(GridPlugin.FUNCTIONCLIENT_KEY);
		String fileHandle = functionClient.registerAgentFile(scriptFile);

		Map<String, String> props = new HashMap<>();
		props.put(ScriptHandler.REMOTE_FILE_ID, fileHandle);
		props.put(ScriptHandler.REMOTE_FILE_VERSION, Long.toString(scriptFile.lastModified()));
		//props.put(ScriptHandler.SCRIPT_FILE, scriptFile.getAbsolutePath());
		
		return props;
	}

	public File getScriptFile(ScriptFunction function) {
		String scriptFilePath = function.getScriptFile().get();
		return new File(scriptFilePath);
	}
	
	protected File getDefaultScriptFile(ScriptFunction function) {
		String filename = getScriptFilename(function);
		String scriptDir = Configuration.getInstance().getProperty("keywords.script.scriptdir");
		File file = new File(scriptDir+"/"+filename);
		return file;
	}

	private String getScriptFilename(ScriptFunction function) {
		TreeSet<String> sortedKeys = new TreeSet<>(function.getAttributes().keySet());
		StringBuilder filename = new StringBuilder();
		Iterator<String> it = sortedKeys.iterator();
		while(it.hasNext()) {
			filename.append(function.getAttributes().get(it.next()));
			if(it.hasNext()) {
				filename.append("_");				
			}
		}
		filename.append(".").append(fileExtensionMap.get(getScriptLanguage(function)));
		return filename.toString();
	}

	public String getScriptLanguage(ScriptFunction conf) {
		return conf.getScriptLanguage().get();
	}
	
	public File setupScriptFile(ScriptFunction function) throws SetupFunctionException {
		File scriptFile;
		
		String scriptFilename = function.getScriptFile().get();
		if(scriptFilename==null || scriptFilename.trim().length()==0) {
			scriptFile = getDefaultScriptFile(function);
			function.getScriptFile().setValue(scriptFile.getAbsolutePath());
		} else {
			scriptFile = new File(scriptFilename);
		}

		if(!scriptFile.exists()) {
			File folder = scriptFile.getParentFile();
			if (!folder.exists()) {
				try {
					Files.createDirectory(folder.toPath());
				} catch (IOException e) {
					throw new SetupFunctionException("Unable to create script folder '"+folder.getAbsolutePath()+"' for function '"+function.getAttributes().get("name"), e);
				}
			}
			try {
				scriptFile.createNewFile();
			} catch (IOException e) {
				throw new SetupFunctionException("Unable to create script folder '"+folder.getAbsolutePath()+"' for function '"+function.getAttributes().get("name"), e);
			}
		} else {
			
		}
		return scriptFile;
	}
	
	public void createScriptFromTemplate(File scriptFile, String templateFilename) throws SetupFunctionException {
		if(scriptFile.exists()) {
			File templateScript = new File(Configuration.getInstance().getProperty("controller.dir")+"/data/templates/"+templateFilename);
			if(templateScript.exists()) {
				try {
					Files.copy(templateScript.toPath(), scriptFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new SetupFunctionException("Unable to copy template '"+templateScript.getAbsolutePath()+"' to '"+scriptFile.getAbsolutePath()+"'", e);
				}				
			} else {
				throw new SetupFunctionException("Unable to apply template. The file '"+templateScript.getAbsolutePath()+"' doesn't exist");
			}
		}		
	}
	
	public void createScriptFromTemplateStream(File scriptFile, InputStream templateScript) throws SetupFunctionException {
		if(scriptFile.exists()) {
			if(templateScript!=null) {
				try {
					Files.copy(templateScript, scriptFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new SetupFunctionException("Unable to copy template from stream to '"+scriptFile.getAbsolutePath()+"'", e);
				}				
			} else {
				throw new SetupFunctionException("Unable to apply template. The stream is null");
			}
		}		
	}
}
