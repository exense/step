/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.plugins.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.core.AbstractStepContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;
import step.grid.filemanager.FileVersion;
import step.handlers.javahandler.KeywordExecutor;
import step.plugins.java.handler.GeneralScriptHandler;
import step.plugins.js223.handler.ScriptHandler;

public abstract class AbstractScriptFunctionType<T extends GeneralScriptFunction> extends AbstractFunctionType<T> {

	private static final Logger log = LoggerFactory.getLogger(AbstractScriptFunctionType.class);
	
	protected Configuration configuration;
	
	public AbstractScriptFunctionType(Configuration configuration) {
		super();
		this.configuration = configuration;
	}
	
	@Override
	public void init() {
		super.init();
		handlerPackageVersion = registerResource(getClass().getClassLoader(), "java-plugin-handler.jar", false, false);
	}

	@Override
	public HandlerProperties getHandlerProperties(T function, AbstractStepContext executionContext) {
		Map<String, String> props = new HashMap<>();
		props.put(ScriptHandler.SCRIPT_LANGUAGE, function.getScriptLanguage().get());
		FileVersion libFileVersion = registerFile(function.getLibrariesFile(), ScriptHandler.LIBRARIES_FILE, props, true, executionContext);
		FileVersion pluginLibFileVersion = addPluginLibsIfRequired(function.getScriptLanguage().get(), props);
		FileVersion scriptFileVersion = registerFile(function.getScriptFile(), ScriptHandler.SCRIPT_FILE, props, true, executionContext);
		FileVersion errorHandlerFileVersion = registerFile(function.getErrorHandlerFile(), ScriptHandler.ERROR_HANDLER_FILE, props, true, executionContext);

		if (configuration.getPropertyAsBoolean("plugins.java.validate.properties")) {
			props.put(KeywordExecutor.VALIDATE_PROPERTIES, Boolean.TRUE.toString());
		}

		return new HandlerProperties(props) {
			@Override
			public void close() throws Exception {
				super.close();
				releaseFile(libFileVersion);
				if (pluginLibFileVersion != null) {
					releaseFile(pluginLibFileVersion);
				}
				releaseFile(scriptFileVersion);
				releaseFile(errorHandlerFileVersion);
			}
		};
	}

	protected FileVersion addPluginLibsIfRequired(String scriptLanguage, Map<String, String> props) {
		String property = configuration.getProperty("plugins."+scriptLanguage+".libs", null);
		if(property != null) {
			return registerFile(new File(property), ScriptHandler.PLUGIN_LIBRARIES_FILE, props, true);
		}
		return null;
	}
	
	@Override
	public String getHandlerChain(GeneralScriptFunction function) {
		return GeneralScriptHandler.class.getName();
	}
	
	public static final Map<String, String> fileExtensionMap = new ConcurrentHashMap<>();
	{
		fileExtensionMap.put("groovy", "groovy");
		fileExtensionMap.put("python", "py");
		fileExtensionMap.put("javascript", "js");
	}
	
	protected File getDefaultScriptFile(GeneralScriptFunction function, String scriptDir) {
		String filename = getScriptFilename(function);
		File file = new File(scriptDir+"/"+filename);
		return file;
	}

	private String getScriptFilename(GeneralScriptFunction function) {
		StringBuilder filename = new StringBuilder();
		if (function.getAttributes().containsKey(AbstractOrganizableObject.NAME)) {
			filename.append(function.getAttributes().get(AbstractOrganizableObject.NAME));
			filename.append("_");
		}
		filename.append(UUID.randomUUID());
		filename.append(".").append(fileExtensionMap.get(getScriptLanguage(function)));
		return filename.toString();
	}

	protected String getScriptLanguage(GeneralScriptFunction conf) {
		return conf.getScriptLanguage().get();
	}

	protected File setupScriptFile(GeneralScriptFunction function, String templateFilename) throws SetupFunctionException {
		File templateScript = new File(configuration.getProperty("controller.dir") + "/data/templates/" + templateFilename);
		try {
			boolean templateExists = templateScript.exists();
			if (!templateExists) {
				log.warn("Default template file not found: " + templateScript.getAbsolutePath());
			}
			return setupScriptFile(function, templateExists ? new FileInputStream(templateScript) : null);
		} catch (FileNotFoundException e) {
			throw new SetupFunctionException("Unable to apply template. The file '" + templateScript.getAbsolutePath() + "' doesn't exist");
		}
	}

	protected File setupScriptFile(GeneralScriptFunction function, InputStream templateStream) throws SetupFunctionException {
		return setupScriptFile(function, templateStream, configuration.getProperty("keywords.script.scriptdir"));
	}
	
	protected File setupScriptFile(GeneralScriptFunction function, InputStream templateStream,
								   String scriptDir) throws SetupFunctionException {
		File scriptFile;
		
		String scriptFilename = function.getScriptFile().get();
		
		if (scriptFilename.startsWith(FileResolver.RESOURCE_PREFIX)) {
			return null;
		}
		
		if(scriptFilename==null || scriptFilename.trim().length()==0) {
			scriptFile = getDefaultScriptFile(function, scriptDir);
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
					throw new SetupFunctionException("Unable to create script folder '"+folder.getAbsolutePath()+"' for function '"+function.getAttributes().get(AbstractOrganizableObject.NAME), e);
				}
			}
			try {
				scriptFile.createNewFile();
			} catch (IOException e) {
				throw new SetupFunctionException("Unable to create script folder '"+folder.getAbsolutePath()+"' for function '"+function.getAttributes().get(AbstractOrganizableObject.NAME), e);
			}
			
			if(templateStream!=null) {
				applyTemplate(scriptFile, templateStream);				
			}
		}
		
		return scriptFile;
	}

	@Override
	public T copyFunction(T function) throws FunctionTypeException {
		T copy = super.copyFunction(function);
		DynamicValue<String> scriptFile = function.getScriptFile();//copy of the source script file
		File newFile = null;
		if(function.getScriptLanguage().get().equals("groovy") || function.getScriptLanguage().get().equals("javascript")) {
			try {
				copy.setScriptFile(new DynamicValue<>(""));//reset script to setup a new one
				String parent = null;
				try {
					parent = new File(scriptFile.get()).getParent();
				} catch (Exception e) {
					//keep configuration script dir in case of error
				}
				newFile = (parent != null) ?
						setupScriptFile(copy, new FileInputStream(scriptFile.get()), parent) :
						setupScriptFile(copy, new FileInputStream(scriptFile.get()));
			} catch (SetupFunctionException | FileNotFoundException e) {
				//Keep source config in case of error
			} finally {
				if (newFile == null) {
					copy.setScriptFile(scriptFile);
				}
			}
		}
		return copy;
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
