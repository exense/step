package step.core.plugins;

import step.attachments.FileResolver;
import step.core.execution.ExecutionContext;
import step.resources.InMemoryResourceAccessor;
import step.resources.LocalResourceManagerImpl;
import step.resources.ResourceAccessor;
import step.resources.ResourceManager;

public class ResourceManagerExecutionPlugin extends AbstractExecutionPlugin {

	@Override
	public void onLocalContextCreation(ExecutionContext context) {
		LocalResourceManagerImpl resourceManager = new LocalResourceManagerImpl();
		context.put(ResourceManager.class, resourceManager);
		
		context.put(FileResolver.class, new FileResolver(resourceManager));
		context.put(ResourceAccessor.class, new InMemoryResourceAccessor());
	}
}
