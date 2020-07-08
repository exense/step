package step.resources;

import java.io.File;

import ch.exense.commons.app.Configuration;
import step.attachments.FileResolver;
import step.core.GlobalContext;
import step.core.accessors.collections.Collection;
import step.core.accessors.collections.CollectionRegistry;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.core.execution.ExecutionContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin()
public class ResourcePlugin extends AbstractControllerPlugin {

	protected ResourceAccessor resourceAccessor;
	protected ResourceRevisionAccessor resourceRevisionAccessor;
	protected ResourceManager resourceManager;
	protected FileResolver fileResolver;
	

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		resourceAccessor = new ResourceAccessorImpl(context.getMongoClientSession());
		resourceRevisionAccessor = new ResourceRevisionAccessorImpl(context.getMongoClientSession());
		String resourceRootDir = getResourceDir(context.getConfiguration());
		resourceManager = new ResourceManagerImpl(new File(resourceRootDir), resourceAccessor, resourceRevisionAccessor);
		context.put(ResourceAccessor.class, resourceAccessor);
		context.put(ResourceManager.class, resourceManager);
		context.getServiceRegistrationCallback().registerService(ResourceServices.class);
		
		fileResolver = new FileResolver(resourceManager);
		context.put(FileResolver.class, fileResolver);
		
		context.get(CollectionRegistry.class).register("resources", new Collection(context.getMongoClientSession().getMongoDatabase(), 
				"resources", Resource.class, true));
		
		context.getEntityManager()
			.register( new Entity<Resource, ResourceAccessor>(
			EntityManager.resources, resourceAccessor, Resource.class, 
			new ResourceImpoter(context)))
			.register(new Entity<ResourceRevision, ResourceRevisionAccessor>(
					EntityManager.resourceRevisions, resourceRevisionAccessor, ResourceRevision.class,
					new ResourceRevisionsImporter(context)));
	}

	public static String getResourceDir(Configuration configuration) {
		String resourceRootDir = configuration.getProperty("resources.dir","resources");
		return resourceRootDir;
	}

	@Override
	public void executionStart(ExecutionContext context) {
		context.put(FileResolver.class, fileResolver);
		context.put(ResourceManager.class, resourceManager);
	}

}
