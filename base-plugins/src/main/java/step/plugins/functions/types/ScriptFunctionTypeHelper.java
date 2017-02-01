package step.plugins.functions.types;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

import step.commons.conf.Configuration;
import step.core.GlobalContext;
import step.functions.Function;
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

	public Map<String, String> getHandlerProperties(Function function) {
		File scriptFile = getScriptFile(function);
		
		FunctionClient functionClient = (FunctionClient) context.get(GridPlugin.FUNCTIONCLIENT_KEY);
		String fileHandle = functionClient.registerAgentFile(scriptFile);

		Map<String, String> props = new HashMap<>();
		props.put(ScriptHandler.REMOTE_FILE_ID, fileHandle);
		props.put(ScriptHandler.REMOTE_FILE_VERSION, Long.toString(scriptFile.lastModified()));
		//props.put(ScriptHandler.SCRIPT_FILE, scriptFile.getAbsolutePath());
		
		return props;
	}

	public File getScriptFile(Function function) {
		String scriptFilePath = function.getConfiguration().getString("scriptFile");
		return new File(scriptFilePath);
	}
	
	protected File getDefaultScriptFile(Function function) {
		String filename = getScriptFilename(function);
		String scriptDir = Configuration.getInstance().getProperty("keywords.script.scriptdir");
		File file = new File(scriptDir+"/"+filename);
		return file;
	}

	private String getScriptFilename(Function function) {
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

	public String getScriptLanguage(Function function) {
		return function.getConfiguration().getString("scriptLanguage");
	}
	
	public File setupScriptFile(Function function) throws SetupFunctionException {
		File scriptFile;
		
		JSONObject conf = function.getConfiguration();
		if(conf.isNull("scriptFile") || conf.getString("scriptFile").trim().length()==0) {
			scriptFile = getDefaultScriptFile(function);
			conf.put("scriptFile", scriptFile.getAbsolutePath());
		} else {
			scriptFile = new File(conf.getString("scriptFile"));
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
}
