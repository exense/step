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
package step.functions.type;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import step.attachments.FileResolver;
import step.core.AbstractStepContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.functions.Function;
import step.functions.io.Input;
import step.grid.GridFileService;
import step.grid.agent.AgentTypes;
import step.grid.filemanager.FileManagerException;
import step.grid.filemanager.FileVersion;
import step.grid.filemanager.FileVersionId;
import step.grid.tokenpool.Interest;
import step.resources.LayeredResourceManager;
import step.resources.ResourceManager;

public abstract class AbstractFunctionType<T extends Function> {
	
	protected FileResolver fileResolver;
	protected LoadingCache<String, File> fileResolverCache;
	
	protected GridFileService gridFileServices;
	
	protected FunctionTypeConfiguration functionTypeConfiguration;

	protected void setFunctionTypeConfiguration(FunctionTypeConfiguration functionTypeConfiguration) {
		this.functionTypeConfiguration = functionTypeConfiguration;
	}

	protected void setFileResolver(FileResolver fileResolver) {
		this.fileResolver = fileResolver;
		
		fileResolverCache = CacheBuilder.newBuilder().concurrencyLevel(functionTypeConfiguration.getFileResolverCacheConcurrencyLevel())
				.maximumSize(functionTypeConfiguration.getFileResolverCacheMaximumsize())
				.expireAfterWrite(functionTypeConfiguration.getFileResolverCacheExpireAfter(), TimeUnit.MILLISECONDS)
				.build(new CacheLoader<String, File>() {
					public File load(String filepath) {
						return fileResolver.resolve(filepath);
					}
				});
		
	}
	
	protected void setGridFileServices(GridFileService gridFileServices) {
		this.gridFileServices = gridFileServices;
	}

	protected void init() {}

	public Map<String, Interest> getTokenSelectionCriteria(T function) {
		Map<String, Interest> criteria = new HashMap<>();
		criteria.put(AgentTypes.AGENT_TYPE_KEY, new Interest(Pattern.compile("default"), true));
		return criteria;
	}
	
	public abstract String getHandlerChain(T function);
	
	public FileVersionId getHandlerPackage(T function) {
		return null;
	};
	
	public abstract Map<String, String> getHandlerProperties(T function, AbstractStepContext executionContext);

	public void beforeFunctionCall(T function, Input<?> input, Map<String, String> properties) throws FunctionExecutionException {
		
	}
	
	public abstract T newFunction();

	public T newFunction(Map<String, String> configuration) {
		return null;
	}

	public void setupFunction(T function) throws SetupFunctionException {
		
	}
	
	public T updateFunction(T function) throws FunctionTypeException {
		return function;
	}
	
	public T copyFunction(T function) throws FunctionTypeException {
		function.setId(null);
		function.getAttributes().put(AbstractOrganizableObject.NAME,function.getAttributes().get(AbstractOrganizableObject.NAME)+"_Copy");
		return function;
	}

	/**
	 * Register the provided file in the grid's file manager for a given property. Enrich the map with the resulting file and version ids.
	 * @deprecated
	 * This method register cleanable resource only, use {@link #registerFile(DynamicValue, String, Map, boolean, AbstractStepContext)} instead
	 * to specifically define whether the registered file can be cleaned up at runtime
	 *
	 * @param dynamicValue the {@link DynamicValue} of the file's path to be registered
	 * @param propertyName the name of the property for which we register the file
	 * @param props the map will be enriched with the propertyName id and version of the registered file that can be later used to retrieve the file
	 * @throws RuntimeException
	 */
	protected void registerFile(DynamicValue<String> dynamicValue, String propertyName, Map<String, String> props) {
		registerFile(dynamicValue, propertyName, props, true, null);
	}

	/**
	 * Register the provided file in the grid's file manager for a given property. Enrich the map with the resulting file and version ids.
	 *
	 * @param dynamicValue     the {@link DynamicValue} of the file's path to be registered
	 * @param propertyName     the name of the property for which we register the file
	 * @param props            the map will be enriched with the propertyName id and version of the registered file that can be later used to retrieve the file
	 * @param cleanable        whether this version of the file can be cleaned-up at runtime
	 * @param executionContext the current execution context (should be defined if the function is executing via ExecutionEngine with
	 *                         execution-scope resource manager)
	 * @throws RuntimeException
	 */
	protected void registerFile(DynamicValue<String> dynamicValue, String propertyName, Map<String, String> props, boolean cleanable, AbstractStepContext executionContext) {
		if(dynamicValue!=null) {
			String filepath = dynamicValue.get();
			if(filepath!=null && filepath.trim().length()>0) {
				File file = null;
				try {
					// in case of isolated execution, the execution context contains temporary in-memory resource manager
					// we have to use this manager instead of the global one from fileResolver
					if (executionContext != null) {
						boolean resolvedFromExecutionContext = false;
						ResourceManager executionContextResourceManager = getResourceManager(executionContext);
						if (executionContextResourceManager == getResourceManager(null)) {
							// if resource manager is the global one, it is better to use fileResolverCache for performance reason
							file = fileResolverCache.get(filepath);
						} else if (executionContextResourceManager instanceof LayeredResourceManager) {
							// performance hack - if the resource manager is layered, but contains the only one resource manager
							// and this resource manager equals to the global one, we can use cached file resolver
							ResourceManager unwrappedManager = unwrapResourceManager((LayeredResourceManager) executionContextResourceManager);
							if (unwrappedManager == getResourceManager(null)) {
								file = fileResolverCache.get(filepath);
							} else {
								resolvedFromExecutionContext = true;
								file = executionContext.getFileResolverCache().get(filepath);
							}
						} else {
							resolvedFromExecutionContext = true;
							file = executionContext.getFileResolverCache().get(filepath);
						}

						// just a fallback - if the file is not found in execution context, try to use global file resolver
						if (resolvedFromExecutionContext && file == null) {
							file = fileResolverCache.get(filepath);
						}
					} else {
						// Using the file resolver cache here to avoid performance issues
						// This method might be called at every function execution
						file = fileResolverCache.get(filepath);
					}
				} catch (ExecutionException e) {
					throw new RuntimeException("Error while resolving path "+filepath, e);
				}
				registerFile(file, propertyName, props, cleanable);
			}			
		}
	}

	/**
	 * Register the provided file in the grid's file manager for a given property. Enrich the map with the resulting file and version ids.
	 * @deprecated
	 * This method register cleanable resource only, use {@link #registerFile(File, String, Map, boolean)} instead
	 * to specifically define whether the registered file can be cleaned up at runtime
	 *
	 * @param file the {@link File} of the resource to be registered
	 * @param propertyName the {@link String} name of the property for which we register the file
	 * @param props the {@link Map} will be enriched with the propertyName id and version of the registered file that can be later used to retrieve the file
	 * @throws RuntimeException
	 */
	protected void registerFile(File file, String propertyName, Map<String, String> props) {
		registerFile(file, propertyName, props, true);
	}

	/**
	 * Register the provided file in the grid's file manager for a given property. Enrich the map with the resulting file and version ids.
	 *
	 * @param file the {@link File} of the resource to be registered
	 * @param propertyName the name of the property for which we register the file
	 * @param props the {@link Map}  will be enriched with the propertyName id and version of the registered file that can be later used to retrieve the file
	 * @param cleanable whether this version of the file can be cleaned-up at runtime
	 * @throws RuntimeException
	 */
	protected void registerFile(File file, String propertyName, Map<String, String> props, boolean cleanable) {
		FileVersionId fileVersionId = registerFile(file, cleanable);
		registerFileVersionId(propertyName, props, fileVersionId);
	}

	private ResourceManager unwrapResourceManager(LayeredResourceManager layeredResourceManager) {
		List<ResourceManager> resourceManagers = layeredResourceManager.getResourceManagers();
		if (resourceManagers.size() != 1) {
			return null;
		} else if (resourceManagers.get(0) instanceof LayeredResourceManager) {
			return unwrapResourceManager((LayeredResourceManager) resourceManagers.get(0));
		} else {
			return resourceManagers.get(0);
		}
	}


	private void registerFileVersionId(String properyName, Map<String, String> props, FileVersionId fileVersionId) {
		props.put(properyName +".id", fileVersionId.getFileId());
		props.put(properyName +".version", fileVersionId.getVersion());
	}

	/**
	 * Register the provided file in the grid's file manager
	 *
	 * @param file the {@link File} of the resource to be registered
	 * @param cleanable whether this version of the file can be cleaned-up at runtime
	 * @return the {@link FileVersionId} of the registered file. The {@link FileVersionId} can be used for later retrieval of this version
	 * @throws RuntimeException
	 */
	protected FileVersionId registerFile(File file, boolean cleanable) {
		FileVersion fileVersion;
		try {
			fileVersion = gridFileServices.registerFile(file, cleanable);
			return fileVersion.getVersionId();
		} catch (FileManagerException e) {
			throw new RuntimeException("Error while registering file "+file.getAbsolutePath(), e);
		}
	}

	/**
	 * Register the provided file as resource in the grid's file manager for a given property. Enrich the map with the resulting file and version ids.
	 * @deprecated
	 * This method register cleanable resource only, use {@link #registerResource(ClassLoader, String, boolean, String, Map, boolean)} instead
	 * to specifically define whether the registered file can be cleaned up at runtime
	 * @param cl the {@link ClassLoader} containing the file as resource
	 * @param resourceName the name of the file's resource
	 * @param isDirectory whether this resource is a directory
	 * @param propertyName the name of the property for which we register the file
	 * @param props the {@link Map}  will be enriched with the propertyName id and version of the registered file that can be later used to retrieve the file
	 * @throws RuntimeException
	 */
	protected void registerResource(ClassLoader cl, String resourceName, boolean isDirectory, String propertyName, Map<String, String> props) {
		registerResource(cl, resourceName, isDirectory, propertyName, props, true);
	}

	/**
	 * Register the provided file as resource in the grid's file manager for a given property. Enrich the map with the resulting file and version ids.
	 *
	 * @param cl the {@link ClassLoader} containing the file as resource
	 * @param resourceName the name of the file's resource
	 * @param isDirectory whether this resource is a directory
	 * @param propertyName the name of the property for which we register the file
	 * @param props the {@link Map}  will be enriched with the propertyName id and version of the registered file that can be later used to retrieve the file
	 * @param cleanable whether this version of the file can be cleaned-up at runtime
	 * @throws RuntimeException
	 */
	protected void registerResource(ClassLoader cl, String resourceName, boolean isDirectory, String propertyName, Map<String, String> props, boolean cleanable) {
		FileVersionId fileVersionId = registerResource(cl, resourceName, isDirectory, cleanable);
		registerFileVersionId(propertyName, props, fileVersionId);
	}

	/**
	 * Register the provided file as resource in the grid's file manager.
	 * @deprecated
	 * This method register non-cleanable resource only, use {@link #registerResource(ClassLoader, String, boolean, boolean)} instead
	 * to specifically define whether the registered file can be cleaned up at runtime
	 * @param cl the {@link ClassLoader} containing the file as resource
	 * @param resourceName the name of the file's resource
	 * @param isDirectory whether this resource is a directory
	 * @return the {@link FileVersionId} of the registered file. The {@link FileVersionId} can be used for later retrieval of this version
	 * @throws RuntimeException
	 */
	protected FileVersionId registerResource(ClassLoader cl, String resourceName, boolean isDirectory) {
		return registerResource(cl, resourceName, isDirectory, false);
	}

	/**
	 * Register the provided file as resource in the grid's file manager.
	 *
	 * @param cl the {@link ClassLoader} containing the file as resource
	 * @param resourceName the name of the file's resource
	 * @param isDirectory whether this resource is a directory
	 * @param cleanable whether this version of the file can be cleaned-up at runtime
	 * @return the {@link FileVersionId} of the registered file. The {@link FileVersionId} can be used for later retrieval of this version
	 * @throws RuntimeException
	 */
	protected FileVersionId registerResource(ClassLoader cl, String resourceName, boolean isDirectory, boolean cleanable) {
		try {
			return gridFileServices.registerFile(cl.getResourceAsStream(resourceName), resourceName, isDirectory, cleanable).getVersionId();
		} catch (FileManagerException e) {
			throw new RuntimeException("Error while registering resource "+resourceName, e);
		}
	}
	
	protected FileVersionId registerFile(String filepath, boolean cleanable) {
		return registerFile(new File(filepath), cleanable);
	}

	protected ResourceManager getResourceManager(AbstractStepContext executionContext) {
		if (executionContext != null && executionContext.getResourceManager() != null) {
			return executionContext.getResourceManager();
		} else if (fileResolver != null) {
			return fileResolver.getResourceManager();
		} else {
			return null;
		}
	}
	
	public void deleteFunction(T function) throws FunctionTypeException {

	}

}
