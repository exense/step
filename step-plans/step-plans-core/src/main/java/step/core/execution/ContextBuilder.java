package step.core.execution;

import org.bson.types.ObjectId;

import ch.exense.commons.app.Configuration;
import step.core.artefacts.InMemoryArtefactAccessor;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.plugins.AbstractExecutionPlugin;
import step.core.plugins.ExecutionCallbacks;
import step.core.plugins.PluginManager;
import step.core.plugins.ResourceManagerExecutionPlugin;
import step.expressions.ExpressionHandler;
import step.threadpool.ThreadPool;

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
		
		context.setArtefactAccessor(new InMemoryArtefactAccessor());
		context.setReportNodeAccessor(new InMemoryReportNodeAccessor());
		
		context.getReportNodeAccessor().save(root);
		
		Configuration configuration = new Configuration();
		context.setConfiguration(configuration);
		
		context.setEventManager(new EventManager());
		context.put(ThreadPool.class, new ThreadPool(context));
		context.setExecutionTypeListener(new ExecutionTypeListener() {
			@Override
			public void updateExecutionType(ExecutionContext context, String newType) {
			}
		});
		
		PluginManager<AbstractExecutionPlugin> pluginManager = new PluginManager<AbstractExecutionPlugin>();
		pluginManager.register(new ResourceManagerExecutionPlugin());
		try {
			pluginManager.initialize();
		} catch (Exception e) {
			throw new ContextCreationException("Error while initializing plugin manager", e);
		}
		
		ExecutionCallbacks pluginManagerProxy = pluginManager.getProxy(ExecutionCallbacks.class);
		context.setExecutionCallbacks(pluginManagerProxy);
		
		context.setExpressionHandler(new ExpressionHandler());
		context.setDynamicBeanResolver(new DynamicBeanResolver(new DynamicValueResolver(context.getExpressionHandler())));
		
		pluginManagerProxy.onLocalContextCreation(context);
		
		return context;
	}
	
	@SuppressWarnings("serial")
	public static class ContextCreationException extends RuntimeException {

		public ContextCreationException(String message, Throwable cause) {
			super(message, cause);
		}
		
	}
}
