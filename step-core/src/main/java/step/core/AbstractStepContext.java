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
		resourceAccessor = parentContext.getResourceAccessor();
		resourceManager = parentContext.getResourceManager();
		fileResolver = parentContext.getFileResolver();
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
