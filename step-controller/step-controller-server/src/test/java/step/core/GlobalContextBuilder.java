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

public class GlobalContextBuilder {

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
					new GenericDBImporter<User, UserAccessor>(context)))
			.register(new Entity<Function, FunctionCRUDAccessor>(
				EntityManager.functions, (FunctionCRUDAccessor) functionAccessor, Function.class, 
				new GenericDBImporter<Function,FunctionCRUDAccessor>(context)));
		
		context.setEventManager(new EventManager());
		
		return context;
	}
}
