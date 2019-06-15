package step.repositories.staging;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin
public class StagingRepositoryPlugin extends AbstractControllerPlugin {

	public static final String STAGING_REPOSITORY = "staging";
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		super.executionControllerStart(context);
		
		StagingContextAccessorImpl registry = new StagingContextAccessorImpl(context.getMongoClientSession());
		StagingRepository repository = new StagingRepository(registry);
		
		context.getRepositoryObjectManager().registerRepository(STAGING_REPOSITORY, repository);
		
		context.put(StagingContextAccessorImpl.class, registry);
		context.getServiceRegistrationCallback().registerService(StagingRepositoryServices.class);
		
	}

}
