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
import step.core.AbstractContext;
import step.core.AbstractStepContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.objectenricher.ObjectEnricher;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;
import step.handlers.javahandler.KeywordExecutor;
import step.plugins.java.handler.GeneralScriptHandler;
import step.plugins.js223.handler.ScriptHandler;
import step.resources.Resource;
import step.resources.ResourceManager;

import static step.resources.ResourceManager.RESOURCE_TYPE_FUNCTIONS;

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
		List<AutoCloseable> createdCloseables = new ArrayList<>();
		try {
			props.put(ScriptHandler.SCRIPT_LANGUAGE, function.getScriptLanguage().get());

			createdCloseables.add(registerFile(function.getLibrariesFile(), ScriptHandler.LIBRARIES_FILE, props, true, executionContext));
			createdCloseables.add(addPluginLibsIfRequired(function.getScriptLanguage().get(), props));
			createdCloseables.add(registerFile(function.getScriptFile(), ScriptHandler.SCRIPT_FILE, props, true, executionContext));
			createdCloseables.add(registerFile(function.getErrorHandlerFile(), ScriptHandler.ERROR_HANDLER_FILE, props, true, executionContext));
			if (configuration.getPropertyAsBoolean("plugins.java.validate.properties")) {
				props.put(KeywordExecutor.VALIDATE_PROPERTIES, Boolean.TRUE.toString());
			}

			return new HandlerProperties(props, createdCloseables);
		} catch (Throwable e) {
			closeRegisteredCloseable(createdCloseables);
			throw e;
		}
	}

	protected AutoCloseable addPluginLibsIfRequired(String scriptLanguage, Map<String, String> props) {
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
		return setupScriptFile(function, templateStream, true);
	}

	protected File setupScriptFile(GeneralScriptFunction function, InputStream templateStream, boolean useResourceManager) throws SetupFunctionException {
		return setupScriptFile(function, templateStream, useResourceManager, configuration.getProperty("keywords.script.scriptdir"));
	}
	
	protected File setupScriptFile(GeneralScriptFunction function, InputStream templateStream,
								   boolean useResourceManager, String scriptDir) throws SetupFunctionException {
		String scriptFilename = function.getScriptFile().get();

		//If the script file is not defined, we create a default file and apply the template
		if(scriptFilename == null || scriptFilename.isBlank()) {
			ResourceManager resourceManager = fileResolver.getResourceManager();
			//If a resource manager is present and should be used by default, we create the script as a resource
			if (resourceManager != null && useResourceManager) {
				String newScriptFilename = getScriptFilename(function);
				InputStream resourceIS = Objects.requireNonNullElse(templateStream, InputStream.nullInputStream());
				// apply context attributes of the function package to the function
				AbstractContext context = new AbstractContext() {};
				try {
					objectHookRegistry.rebuildContext(context, function);
					ObjectEnricher objectEnricher = objectHookRegistry.getObjectEnricher(context);
					Resource resource = resourceManager.createResource(RESOURCE_TYPE_FUNCTIONS, resourceIS, newScriptFilename, false, objectEnricher);
					function.getScriptFile().setValue(fileResolver.createPathForResourceId(resource.getId().toHexString()));
				} catch (Exception e) {
					throw new SetupFunctionException("Unable to create the default script resource", e);
				}
			} else {
				//If resource manager is not available or an absolute path is requested, we create a new file and use it directly as script file
				function.getScriptFile().setValue(getDefaultScriptFile(function, scriptDir).getAbsolutePath());
			}
		}

		//In all cases, if the files doesn't exist yet, we create it with the provided template
		File scriptFile = fileResolver.resolve(function.getScriptFile().get());
		if(scriptFile != null && !scriptFile.exists()) {
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
				String scriptFileValue = scriptFile.get();
				try {
					parent = new File(scriptFileValue).getParent();
				} catch (Exception e) {
					//keep configuration script dir in case of error
				}
				boolean isResource = fileResolver.isResource(scriptFileValue);
				if (isResource) {
					scriptFileValue = fileResolver.resolve(scriptFileValue).getAbsolutePath();
				}
				newFile = (parent != null) ?
						setupScriptFile(copy, new FileInputStream(scriptFileValue), isResource, parent) :
						setupScriptFile(copy, new FileInputStream(scriptFileValue), isResource);
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
