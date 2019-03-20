package step.plugins.java;

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
import step.functions.Function;
import step.functions.type.AbstractFunctionType;
import step.functions.type.SetupFunctionException;
import step.grid.bootstrap.ResourceExtractor;
import step.grid.filemanager.FileVersionId;
import step.plugins.java.handler.GeneralScriptHandler;
import step.plugins.js223.handler.ScriptHandler;

public abstract class AbstractScriptFunctionType<T extends GeneralScriptFunction> extends AbstractFunctionType<T> {

	protected File daemonJar;
	
	protected Configuration configuration;
	
	@Override
	public void init() {
		super.init();
		daemonJar = ResourceExtractor.extractResource(getClass().getClassLoader(), "java-plugin-handler.jar");
		configuration = Configuration.getInstance();
	}
	
	public Map<String, String> getHandlerProperties(T function) {
		Map<String, String> props = new HashMap<>();
		props.put(ScriptHandler.SCRIPT_LANGUAGE, function.getScriptLanguage().get());		
		registerFile(function.getLibrariesFile(), ScriptHandler.LIBRARIES_FILE, props);
		addPluginLibsIfRequired(function.getScriptLanguage().get(), props);
		registerFile(function.getScriptFile(), ScriptHandler.SCRIPT_FILE, props);
		registerFile(function.getErrorHandlerFile(), ScriptHandler.ERROR_HANDLER_FILE, props);
		return props;
	}

	protected void addPluginLibsIfRequired(String scriptLanguage, Map<String, String> props) {
		String property = configuration.getProperty("plugins."+scriptLanguage+".libs", null);
		if(property != null) {
			registerFile(new File(property), ScriptHandler.PLUGIN_LIBRARIES_FILE, props);
		}
	}
	
	@Override
	public String getHandlerChain(GeneralScriptFunction function) {
		return GeneralScriptHandler.class.getName();
	}

	@Override
	public FileVersionId getHandlerPackage(GeneralScriptFunction function) {
		return registerFile(daemonJar.getAbsoluteFile());
	}
	
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

	protected String getScriptLanguage(GeneralScriptFunction conf) {
		return conf.getScriptLanguage().get();
	}
	
	protected File setupScriptFile(GeneralScriptFunction function, String templateFilename) throws SetupFunctionException {
		File templateScript = new File(Configuration.getInstance().getProperty("controller.dir")+"/data/templates/"+templateFilename);
		try {
			return setupScriptFile(function,new FileInputStream(templateScript));
		} catch (FileNotFoundException e) {
			throw new SetupFunctionException("Unable to apply template. The file '"+templateScript.getAbsolutePath()+"' doesn't exist");
		}
	}
	
	protected File setupScriptFile(GeneralScriptFunction function, InputStream templateStream) throws SetupFunctionException {
		File scriptFile;
		
		String scriptFilename = function.getScriptFile().get();
		
		if (scriptFilename.startsWith(FileResolver.RESOURCE_PREFIX)) {
			return null;
		}
		
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
	
	public File getScriptFile(T function) {
		String scriptFilePath = function.getScriptFile().get();
		return fileResolver.resolve(scriptFilePath);
	}
}
