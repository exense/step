package step.repositories;

import step.core.execution.ExecutionEngineContext;
import step.core.plugins.Plugin;
import step.core.repositories.RepositoryObjectReference;
import step.engine.plugins.AbstractExecutionEnginePlugin;

@Plugin
public class LocalRepositoryPlugin extends AbstractExecutionEnginePlugin {

	@Override
	public void initialize(ExecutionEngineContext context) {
		context.getRepositoryObjectManager().registerRepository(RepositoryObjectReference.LOCAL_REPOSITORY_ID, new LocalRepository(context.getPlanAccessor()));
	}

}
