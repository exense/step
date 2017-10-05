package step.plugins.functions.types;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import step.attachments.FileResolver;
import step.commons.conf.Configuration;
import step.commons.helpers.FileHelper;
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

	public Map<String, String> getHandlerProperties(GeneralScriptFunction function) {
		File scriptFile = getScriptFile(function);
		
		FunctionClient functionClient = (FunctionClient) context.get(GridPlugin.FUNCTIONCLIENT_KEY);
		String fileHandle = functionClient.registerAgentFile(scriptFile);

		Map<String, String> props = new HashMap<>();
		props.put(ScriptHandler.REMOTE_FILE_ID, fileHandle);
		props.put(ScriptHandler.REMOTE_FILE_VERSION, Long.toString(FileHelper.getLastModificationDateRecursive(scriptFile)));					

		props.put(ScriptHandler.SCRIPT_LANGUAGE, function.getScriptLanguage().get());
		//props.put(ScriptHandler.SCRIPT_FILE, scriptFile.getAbsolutePath());
		
		return props;
	}

	public File getScriptFile(GeneralScriptFunction function) {
		String scriptFilePath = function.getScriptFile().get();
		FileResolver fileResolver = new FileResolver(context.getAttachmentManager());
		return fileResolver.resolve(scriptFilePath);
	}
	
	protected File getDefaultScriptFile(GeneralScriptFunction function) {
		String filename = getScriptFilename(function);
		String scriptDir = Configuration.getInstance().getProperty("keywords.script.scriptdir");
		File file = new File(scriptDir+"/"+filename);
		return file;
	}

	private String getScriptFilename(GeneralScriptFunction function) {
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

	public String getScriptLanguage(GeneralScriptFunction conf) {
		return conf.getScriptLanguage().get();
	}
	
	public File setupScriptFile(GeneralScriptFunction function, String templateFilename) throws SetupFunctionException {
		File templateScript = new File(Configuration.getInstance().getProperty("controller.dir")+"/data/templates/"+templateFilename);
		try {
			return setupScriptFile(function,new FileInputStream(templateScript));
		} catch (FileNotFoundException e) {
			throw new SetupFunctionException("Unable to apply template. The file '"+templateScript.getAbsolutePath()+"' doesn't exist");
		}
	}
	
	public File setupScriptFile(GeneralScriptFunction function, InputStream templateStream) throws SetupFunctionException {
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
					throw new SetupFunctionException("Unable to create script folder '"+folder.getAbsolutePath()+"' for function '"+function.getAttributes().get(Function.NAME), e);
				}
			}
			try {
				scriptFile.createNewFile();
			} catch (IOException e) {
				throw new SetupFunctionException("Unable to create script folder '"+folder.getAbsolutePath()+"' for function '"+function.getAttributes().get(Function.NAME), e);
			}
			
			if(templateStream!=null) {
				applyTemplate(scriptFile, templateStream);				
			}
		}
		
		return scriptFile;
	}
	
	private void applyTemplate(File scriptFile, InputStream templateScript) throws SetupFunctionException {
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
