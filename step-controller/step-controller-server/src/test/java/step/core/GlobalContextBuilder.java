package step.core;

import ch.exense.commons.app.Configuration;
import step.core.accessors.CollectionRegistry;
import step.core.artefacts.InMemoryArtefactAccessor;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.execution.EventManager;
import step.core.execution.InMemoryExecutionAccessor;
import step.core.plugins.ControllerPluginManager;
import step.core.repositories.RepositoryObjectManager;
import step.core.scheduler.InMemoryExecutionTaskAccessor;
import step.expressions.ExpressionHandler;

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
		context.setArtefactAccessor(new InMemoryArtefactAccessor());
		context.setReportAccessor(new InMemoryReportNodeAccessor());
		context.setScheduleAccessor(new InMemoryExecutionTaskAccessor());
		context.setRepositoryObjectManager(new RepositoryObjectManager(context.getArtefactAccessor()));
		
		context.setEventManager(new EventManager());
		
		return context;
	}
}
