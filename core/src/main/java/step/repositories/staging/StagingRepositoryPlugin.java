package step.repositories.staging;

import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

@Plugin
public class StagingRepositoryPlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		super.executionControllerStart(context);
		
		StagingContextRegistry registry = new StagingContextRegistry();
		StagingRepository repository = new StagingRepository(registry);
		
		context.getRepositoryObjectManager().registerRepository("local-isolated", repository);
		
		context.put(StagingContextRegistry.class, registry);
		context.getServiceRegistrationCallback().registerService(StagingRepositoryServices.class);
		
	}

}
