package step.core.execution;

import org.bson.types.ObjectId;

import step.attachments.FileResolver;
import ch.exense.commons.app.Configuration;
import step.core.artefacts.InMemoryArtefactAccessor;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.plugins.ExecutionCallbacks;
import step.expressions.ExpressionHandler;
import step.resources.LocalResourceManagerImpl;
import step.resources.ResourceManager;
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
		
		context.setExecutionCallbacks(new ExecutionCallbacks() {
			
			@Override
			public void unassociateThread(ExecutionContext context, Thread thread) {
			}
			
			@Override
			public void executionStart(ExecutionContext context) {
			}
			
			@Override
			public void beforeReportNodeExecution(ExecutionContext context, ReportNode node) {
			}
			
			@Override
			public void beforeExecutionEnd(ExecutionContext context) {
			}
			
			@Override
			public void associateThread(ExecutionContext context, Thread thread) {
			}
			
			@Override
			public void afterReportNodeSkeletonCreation(ExecutionContext context, ReportNode node) {
			}
			
			@Override
			public void afterReportNodeExecution(ExecutionContext context, ReportNode node) {
			}
			
			@Override
			public void afterExecutionEnd(ExecutionContext context) {
			}
		});
		
		context.setExpressionHandler(new ExpressionHandler());
		context.setDynamicBeanResolver(new DynamicBeanResolver(new DynamicValueResolver(context.getExpressionHandler())));
		
		LocalResourceManagerImpl resourceManager = new LocalResourceManagerImpl();
		context.put(ResourceManager.class, resourceManager);
		context.put(FileResolver.class, new FileResolver(resourceManager));
		
		return context;
	}
}
