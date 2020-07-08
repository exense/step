package step.core;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.exense.commons.app.Configuration;
import ch.exense.commons.io.FileHelper;
import step.attachments.FileResolver;
import step.core.access.InMemoryUserAccessor;
import step.core.access.User;
import step.core.access.UserAccessor;
import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.collections.CollectionRegistry;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.core.execution.EventManager;
import step.core.execution.InMemoryExecutionAccessor;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.imports.GenericDBImporter;
import step.core.imports.PlanImporter;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plugins.ControllerPluginManager;
import step.core.repositories.RepositoryObjectManager;
import step.core.scheduler.ExecutionTaskAccessor;
import step.core.scheduler.ExecutiontTaskParameters;
import step.core.scheduler.InMemoryExecutionTaskAccessor;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.FunctionCRUDAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.resources.InMemoryResourceAccessor;
import step.resources.InMemoryResourceRevisionAccessor;
import step.resources.Resource;
import step.resources.ResourceAccessor;
import step.resources.ResourceManager;
import step.resources.ResourceManagerImpl;
import step.resources.ResourceRevision;
import step.resources.ResourceRevisionAccessor;
import step.resources.ResourceRevisionsImporter;

public class GlobalContextBuilder {
	
	private static final Logger logger = LoggerFactory.getLogger(GlobalContextBuilder.class);

	public static GlobalContext createGlobalContext() {
		GlobalContext context = new GlobalContext();

		context.setExpressionHandler(new ExpressionHandler());
		context.setDynamicBeanResolver(new DynamicBeanResolver(new DynamicValueResolver(context.getExpressionHandler())));
		
		Configuration configuration = new Configuration();
		ControllerPluginManager pluginManager = new ControllerPluginManager(configuration);
		context.setPluginManager(pluginManager);
		
		context.setConfiguration(configuration);
		
		context.put(CollectionRegistry.class, new CollectionRegistry());
		context.setExecutionAccessor(new InMemoryExecutionAccessor());
		context.setPlanAccessor(new InMemoryPlanAccessor());
		context.setReportAccessor(new InMemoryReportNodeAccessor());
		context.setScheduleAccessor(new InMemoryExecutionTaskAccessor());
		context.setUserAccessor(new InMemoryUserAccessor());
		context.setRepositoryObjectManager(new RepositoryObjectManager(context.getPlanAccessor()));
		
		FunctionAccessor functionAccessor = new InMemoryFunctionAccessorImpl();
		context.put(FunctionAccessor.class, functionAccessor);
		
		ResourceAccessor resourceAccessor = new InMemoryResourceAccessor();
		InMemoryResourceRevisionAccessor resourceRevisionAccessor = new InMemoryResourceRevisionAccessor();
		try {
			File rootFolder = FileHelper.createTempFolder();
			ResourceManager resourceManager = new ResourceManagerImpl(rootFolder,resourceAccessor, resourceRevisionAccessor);
			FileResolver fileResolver = new FileResolver(resourceManager);
			context.put(ResourceAccessor.class, resourceAccessor);
			context.put(ResourceManager.class, resourceManager);
			context.put(FileResolver.class, fileResolver);
		} catch (IOException e) {
			logger.error("Unable to create temp folder for the resource manager", e);
		}
		
		context.setEntityManager(new EntityManager(context));
		context.getEntityManager().register( new Entity<Execution, ExecutionAccessor>(
				EntityManager.executions, context.getExecutionAccessor(), Execution.class, 
				new GenericDBImporter<Execution, ExecutionAccessor>(context) {
			}))
			.register( new Entity<Plan,PlanAccessor>(EntityManager.plans, context.getPlanAccessor(), Plan.class, new PlanImporter(context)))
			.register(new Entity<ReportNode,ReportNodeAccessor>(
					EntityManager.reports, context.getReportAccessor(), ReportNode.class,
					new GenericDBImporter<ReportNode, ReportNodeAccessor>(context)))
			.register(new Entity<ExecutiontTaskParameters,ExecutionTaskAccessor>(
					EntityManager.tasks, context.getScheduleAccessor(), ExecutiontTaskParameters.class, 
					new GenericDBImporter<ExecutiontTaskParameters, ExecutionTaskAccessor>(context)))
			.register(new Entity<User,UserAccessor>(
					EntityManager.users, context.getUserAccessor(), User.class, 
					new GenericDBImporter<User, UserAccessor>(context)))
			.register(new Entity<Function, FunctionCRUDAccessor>(
				EntityManager.functions, (FunctionCRUDAccessor) functionAccessor, Function.class, 
				new GenericDBImporter<Function,FunctionCRUDAccessor>(context)))
			.register( new Entity<Resource, ResourceAccessor>(
				EntityManager.resources, resourceAccessor, Resource.class, 
				new GenericDBImporter<Resource, ResourceAccessor>(context) {
				}))
			.register(new Entity<ResourceRevision, ResourceRevisionAccessor>(
					EntityManager.resourceRevisions, resourceRevisionAccessor, ResourceRevision.class,
					new ResourceRevisionsImporter(context)));
		
		context.setEventManager(new EventManager());
		
		return context;
	}
}
