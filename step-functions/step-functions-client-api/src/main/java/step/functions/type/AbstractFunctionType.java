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
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import step.attachments.FileResolver;
import ch.exense.commons.core.model.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.functions.Function;
import step.functions.io.Input;
import step.grid.GridFileService;
import step.grid.agent.AgentTypes;
import step.grid.filemanager.FileManagerException;
import step.grid.filemanager.FileVersion;
import step.grid.filemanager.FileVersionId;
import step.grid.tokenpool.Interest;

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
	
	public abstract Map<String, String> getHandlerProperties(T function);
	
	public void beforeFunctionCall(T function, Input<?> input, Map<String, String> properties) throws FunctionExecutionException {
		
	}
	
	public abstract T newFunction();
	
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
	
	protected void registerFile(DynamicValue<String> dynamicValue, String properyName, Map<String, String> props) {
		if(dynamicValue!=null) {
			String filepath = dynamicValue.get();
			if(filepath!=null && filepath.trim().length()>0) {
				File file;
				try {
					// Using the file resolver cache here to avoid performance issues
					// This method might be called at every function execution
					file = fileResolverCache.get(filepath);
				} catch (ExecutionException e) {
					throw new RuntimeException("Error while resolving path "+filepath, e);
				}
				registerFile(file, properyName, props);			
			}			
		}
	}
	
	protected void registerFile(File file, String properyName, Map<String, String> props) {
		FileVersionId fileVersionId = registerFile(file);
		props.put(properyName+".id", fileVersionId.getFileId());
		props.put(properyName+".version", fileVersionId.getVersion());
	}
	
	protected FileVersionId registerFile(File file) {
		FileVersion fileVersion;
		try {
			fileVersion = gridFileServices.registerFile(file);
			return fileVersion.getVersionId();
		} catch (FileManagerException e) {
			throw new RuntimeException("Error while registering file "+file.getAbsolutePath(), e);
		}
	}
	
	protected FileVersionId registerResource(ClassLoader cl, String resourceName, boolean isDirectory) {
		try {
			return gridFileServices.registerFile(cl.getResourceAsStream(resourceName), resourceName, isDirectory).getVersionId();
		} catch (FileManagerException e) {
			throw new RuntimeException("Error while registering resource "+resourceName, e);
		}
	}
	
	protected FileVersionId registerFile(String filepath) {
		return registerFile(new File(filepath));
	}
	
	public void deleteFunction(T function) throws FunctionTypeException {

	}
}
