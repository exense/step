package step.core;

import ch.exense.commons.app.Configuration;
import step.core.access.InMemoryUserAccessor;
import step.core.access.User;
import step.core.access.UserAccessor;
import step.core.accessors.collections.CollectionRegistry;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.core.execution.ExecutionManagerImpl;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.InMemoryExecutionAccessor;
import step.core.imports.GenericDBImporter;
import step.core.imports.PlanImporter;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plugins.ControllerPluginManager;
import step.core.plugins.PluginManager.Builder.CircularDependencyException;
import step.core.repositories.RepositoryObjectManager;
import step.core.scheduler.ExecutionTaskAccessor;
import step.core.scheduler.ExecutiontTaskParameters;
import step.core.scheduler.InMemoryExecutionTaskAccessor;
import step.expressions.ExpressionHandler;

public class GlobalContextBuilder {

	public static GlobalContext createGlobalContext() throws CircularDependencyException, InstantiationException, IllegalAccessException {
		GlobalContext context = new GlobalContext();

		context.setExpressionHandler(new ExpressionHandler());
		context.setDynamicBeanResolver(new DynamicBeanResolver(new DynamicValueResolver(context.getExpressionHandler())));
		
		Configuration configuration = new Configuration();
		ControllerPluginManager pluginManager = new ControllerPluginManager(configuration);
		context.setPluginManager(pluginManager);
		
		context.setConfiguration(configuration);
		
		context.put(CollectionRegistry.class, new CollectionRegistry());
		InMemoryExecutionAccessor executionAccessor = new InMemoryExecutionAccessor();
		context.setExecutionAccessor(executionAccessor);
		context.setExecutionManager(new ExecutionManagerImpl(executionAccessor));
		context.setPlanAccessor(new InMemoryPlanAccessor());
		context.setReportNodeAccessor(new InMemoryReportNodeAccessor());
		context.setScheduleAccessor(new InMemoryExecutionTaskAccessor());
		context.setUserAccessor(new InMemoryUserAccessor());
		context.setRepositoryObjectManager(new RepositoryObjectManager());
		
		context.setEntityManager(new EntityManager());
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
					new GenericDBImporter<User, UserAccessor>(context)));
		
		return context;
	}
}
