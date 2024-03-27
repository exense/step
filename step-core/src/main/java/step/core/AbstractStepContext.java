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
package step.core;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import step.attachments.FileResolver;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.expressions.ExpressionHandler;
import step.resources.*;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class AbstractStepContext extends AbstractContext {

	private final UUID contextId = UUID.randomUUID();
	private ExpressionHandler expressionHandler;
	private DynamicBeanResolver dynamicBeanResolver;
	private ResourceManager resourceManager;
	private FileResolver fileResolver;
	private LoadingCache<String, File> fileResolverCache;
	// Keep track of the default resource manager created at initialization of the context
	private LocalResourceManagerImpl localResourceManager;

	protected void setDefaultAttributes() {
		expressionHandler = new ExpressionHandler();
		dynamicBeanResolver = new DynamicBeanResolver(new DynamicValueResolver(expressionHandler));
		// Create a local resource manager in a dedicated folder per default
		localResourceManager = new LocalResourceManagerImpl(getContextFolderAsFile(), new InMemoryResourceAccessor(), new InMemoryResourceRevisionAccessor());
		setResourceManager(localResourceManager);
	}

	private File getContextFolderAsFile() {
		//TODO: currently, this will create a directory relative to $CWD.
		// We may (or may not) want to consolidate this, e.g. to use a common temporary dir.
		String dirName =  "stepContext_" + getClass().getSimpleName() + "_" + contextId;
		return new File(dirName);
	}

	protected void useSourceAttributesFromParentContext(AbstractStepContext parentContext) {
		ResourceManager resourceManager = new LayeredResourceManager(parentContext.getResourceManager(), true);
		setResourceManager(resourceManager);
	}

	protected void useStandardAttributesFromParentContext(AbstractStepContext parentContext) {
		expressionHandler = parentContext.getExpressionHandler();
		dynamicBeanResolver = parentContext.getDynamicBeanResolver();
	}

	public ExpressionHandler getExpressionHandler() {
		return expressionHandler;
	}

	public void setExpressionHandler(ExpressionHandler expressionHandler) {
		this.expressionHandler = expressionHandler;
	}

	public DynamicBeanResolver getDynamicBeanResolver() {
		return dynamicBeanResolver;
	}

	public void setDynamicBeanResolver(DynamicBeanResolver dynamicBeanResolver) {
		this.dynamicBeanResolver = dynamicBeanResolver;
	}

	public ResourceManager getResourceManager() {
		return resourceManager;
	}

	public void setResourceManager(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
		updateFileResolver();
	}

	public FileResolver getFileResolver() {
		return fileResolver;
	}

	private void updateFileResolver() {
		this.fileResolver = new FileResolver(resourceManager);
		this.fileResolverCache = CacheBuilder.newBuilder().concurrencyLevel(4)
				.maximumSize(1000)
				.expireAfterWrite(500, TimeUnit.MILLISECONDS)
				.build(new CacheLoader<>() {
					public File load(String filepath) {
						return fileResolver.resolve(filepath);
					}
				});
	}

	public LoadingCache<String, File> getFileResolverCache() {
		return fileResolverCache;
	}

	@Override
	public void close() throws IOException {
		// Cleanup the default resource manager
		localResourceManager.cleanup();
		super.close();
	}
}
