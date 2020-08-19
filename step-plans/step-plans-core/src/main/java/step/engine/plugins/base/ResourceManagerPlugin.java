package step.engine.plugins.base;

import step.attachments.FileResolver;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.resources.LocalResourceManagerImpl;
import step.resources.ResourceManager;

@Plugin
public class ResourceManagerPlugin extends AbstractExecutionEnginePlugin {

	private ResourceManager resourceManager;
	private FileResolver fileResolver;
	
	@Override
	public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
		resourceManager = context.inheritFromParentOrComputeIfAbsent(parentContext, ResourceManager.class, k->new LocalResourceManagerImpl());
		fileResolver = context.inheritFromParentOrComputeIfAbsent(parentContext, FileResolver.class, k->new FileResolver(resourceManager));
	}

	@Override
	public void initializeExecutionContext(ExecutionEngineContext executionEngineContext,
			ExecutionContext executionContext) {
		executionContext.put(ResourceManager.class, resourceManager);
		executionContext.put(FileResolver.class, fileResolver);
	}
}
