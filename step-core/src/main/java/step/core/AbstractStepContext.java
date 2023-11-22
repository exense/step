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

import step.attachments.FileResolver;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.expressions.ExpressionHandler;
import step.resources.*;

import java.io.File;

public abstract class AbstractStepContext extends AbstractContext {

	private ExpressionHandler expressionHandler;
	private DynamicBeanResolver dynamicBeanResolver;
	private ResourceAccessor resourceAccessor;
	private ResourceManager resourceManager;
	private FileResolver fileResolver;

	protected void setDefaultAttributes() {
		expressionHandler = new ExpressionHandler();
		dynamicBeanResolver = new DynamicBeanResolver(new DynamicValueResolver(expressionHandler));
		resourceAccessor = new InMemoryResourceAccessor();
		resourceManager = new LocalResourceManagerImpl(new File("resources"), resourceAccessor, new InMemoryResourceRevisionAccessor());
		fileResolver = new FileResolver(resourceManager);
	}

	protected void useSourceAttributesFromParentContext(AbstractStepContext parentContext) {
		LayeredResourceAccessor layeredAccessor = new LayeredResourceAccessor();
		layeredAccessor.pushAccessor(parentContext.getResourceAccessor());
		resourceAccessor = layeredAccessor;

		resourceManager = new LayeredResourceManager(parentContext.getResourceManager());

		fileResolver = new FileResolver(resourceManager);
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

	public ResourceAccessor getResourceAccessor() {
		return resourceAccessor;
	}

	public void setResourceAccessor(ResourceAccessor resourceAccessor) {
		this.resourceAccessor = resourceAccessor;
	}

	public ResourceManager getResourceManager() {
		return resourceManager;
	}

	public void setResourceManager(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}

	public FileResolver getFileResolver() {
		return fileResolver;
	}

	public void setFileResolver(FileResolver fileResolver) {
		this.fileResolver = fileResolver;
	}
}
