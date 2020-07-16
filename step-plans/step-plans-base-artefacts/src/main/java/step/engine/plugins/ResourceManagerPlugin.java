package step.engine.plugins;

import step.attachments.FileResolver;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.Plugin;
import step.resources.LocalResourceManagerImpl;
import step.resources.ResourceManager;

@Plugin
public class ResourceManagerPlugin extends AbstractExecutionEnginePlugin {

	@Override
	public void initialize(ExecutionEngineContext context) {
		ResourceManager resourceManager = context.computeIfAbsent(ResourceManager.class, k->new LocalResourceManagerImpl());
		context.computeIfAbsent(FileResolver.class, k->new FileResolver(resourceManager));
	}
}
