package step.core.execution;

import org.bson.types.ObjectId;

import step.attachments.FileResolver;
import step.commons.conf.Configuration;
import step.core.GlobalContext;
import step.core.artefacts.InMemoryArtefactAccessor;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.plugins.PluginManager;
import step.core.repositories.RepositoryObjectManager;
import step.expressions.ExpressionHandler;
import step.resources.LocalResourceManagerImpl;
import step.resources.ResourceManager;

public class ContextBuilder {
	
	public static ExecutionContext createLocalExecutionContext() {
		return createLocalExecutionContext(new ObjectId().toString());
	}
	
	public static ExecutionContext createLocalExecutionContext(String executionId) {
		ExecutionContext context = new ExecutionContext(executionId);
		
		ReportNode root = new ReportNode();
		root.setId(new ObjectId(context.getExecutionId()));
		context.getReportNodeCache().put(root);
		context.setReport(root);
		context.setCurrentReportNode(root);
		context.setExecutionParameters(new ExecutionParameters("dummy", null, ExecutionMode.RUN));
		
		context.setExecutionAccessor(new InMemoryExecutionAccessor());
		context.setArtefactAccessor(new InMemoryArtefactAccessor());
		context.setReportNodeAccessor(new InMemoryReportNodeAccessor());
		
		context.getReportNodeAccessor().save(root);
		
		Configuration configuration = new Configuration();
		context.setConfiguration(configuration);
		context.setRepositoryObjectManager(new RepositoryObjectManager(context.getArtefactAccessor()));
		
		context.setEventManager(new EventManager());
		
		PluginManager pluginManager = new PluginManager();
		context.setExecutionCallbacks(pluginManager.getProxy());
		
		context.setExpressionHandler(new ExpressionHandler());
		context.setDynamicBeanResolver(new DynamicBeanResolver(new DynamicValueResolver(context.getExpressionHandler())));
		
		LocalResourceManagerImpl resourceManager = new LocalResourceManagerImpl();
		context.put(ResourceManager.class, resourceManager);
		context.put(FileResolver.class, new FileResolver(resourceManager));
		
		return context;
	}
	
	public static ExecutionContext createExecutionContext(GlobalContext globalContext) {
		ExecutionContext context = new ExecutionContext(new ObjectId().toString());
		context.setExecutionParameters(new ExecutionParameters("dummy", null, ExecutionMode.RUN));
		context.setExpressionHandler(globalContext.getExpressionHandler());
		context.setDynamicBeanResolver(globalContext.getDynamicBeanResolver());
		context.setConfiguration(globalContext.getConfiguration());
		context.setExecutionAccessor(globalContext.getExecutionAccessor());
		context.setArtefactAccessor(globalContext.getArtefactAccessor());
		context.setReportNodeAccessor(globalContext.getReportAccessor());
		context.setRepositoryObjectManager(globalContext.getRepositoryObjectManager());
		context.setEventManager(globalContext.getEventManager());
		context.setExecutionCallbacks(globalContext.getPluginManager().getProxy());
		return context;
	}
}
