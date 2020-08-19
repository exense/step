package step.resources;

import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.accessors.collections.Collection;
import step.core.accessors.collections.CollectionRegistry;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin()
public class ResourceManagerControllerPlugin extends AbstractControllerPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		context.getServiceRegistrationCallback().registerService(ResourceServices.class);
		context.get(CollectionRegistry.class).register("resources", new Collection<Resource>(context.getMongoClientSession().getMongoDatabase(), 
				"resources", Resource.class, true));
	}

	public static String getResourceDir(Configuration configuration) {
		String resourceRootDir = configuration.getProperty("resources.dir","resources");
		return resourceRootDir;
	}
}
