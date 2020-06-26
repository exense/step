package step.core.execution;

import javax.json.JsonObject;

import org.bson.types.ObjectId;

import ch.exense.commons.app.Configuration;
import step.attachments.FileResolver;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.PlanAccessor;
import step.core.plugins.ExecutionCallbacks;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.io.Output;
import step.resources.LocalResourceManagerImpl;
import step.resources.ResourceManager;
import step.threadpool.ThreadPool;

public class ExecutionContextBuilder {
	
	protected final ExecutionContext executionContext;

	public ExecutionContextBuilder() {
		this(new ObjectId().toString());
	}
	
	public ExecutionContextBuilder(String executionId) {
		this(executionId, new ExecutionParameters("dummy", null, ExecutionMode.RUN));
	}
	
	public ExecutionContextBuilder(String executionId, ExecutionParameters executionParameters) {
		super();
		executionContext = new ExecutionContext(executionId, executionParameters);
	}

	public ExecutionContextBuilder withPlanAccessor(PlanAccessor planAccessor) {
		executionContext.setPlanAccessor(planAccessor);
		return this;
	}
	
	public ExecutionContextBuilder withReportNodeAccessor(ReportNodeAccessor reportNodeAccessor) {
		executionContext.setReportNodeAccessor(reportNodeAccessor);
		return this;
	}
	
	/**
	 * Configures an {@link ExecutionContext} for local executions (in opposition to 
	 * controller executions performed on a controller server) with in memory accessors.
	 *  
	 * @return the {@link ExecutionContextBuilder}
	 */
	public ExecutionContextBuilder configureForlocalExecution() {
		executionContext.setPlanAccessor(new InMemoryPlanAccessor());
		InMemoryReportNodeAccessor reportNodeAccessor = new InMemoryReportNodeAccessor();
		executionContext.setReportNodeAccessor(reportNodeAccessor);

		reportNodeAccessor.save(executionContext.getReport());
		
		executionContext.setConfiguration(new Configuration());
		executionContext.setEventManager(new EventManager());
		executionContext.put(ThreadPool.class, new ThreadPool(executionContext));
		executionContext.setExecutionTypeListener(new ExecutionTypeListener() {
			@Override
			public void updateExecutionType(ExecutionContext context, String newType) {
			}
		});
		
		executionContext.setExecutionCallbacks(new ExecutionCallbacks() {
			
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
			public void rollbackReportNode(ExecutionContext context, ReportNode node) {}
			
			@Override
			public void afterExecutionEnd(ExecutionContext context) {
			}

			@Override
			public void beforeFunctionExecution(ExecutionContext context, ReportNode node, Function function) {
			}

			@Override
			public void afterFunctionExecution(ExecutionContext context, ReportNode node, Function function, Output<JsonObject> output) {
			}

			@Override
			public void beforePlanImport(ExecutionContext context) {
			}

			@Override
			public void associateThread(ExecutionContext context, Thread thread, long parentThreadId) {
			}
		});
		
		ExpressionHandler expressionHandler = new ExpressionHandler();
		executionContext.setExpressionHandler(expressionHandler);
		executionContext.setDynamicBeanResolver(new DynamicBeanResolver(new DynamicValueResolver(expressionHandler)));
		
		LocalResourceManagerImpl resourceManager = new LocalResourceManagerImpl();
		executionContext.put(ResourceManager.class, resourceManager);
		executionContext.put(FileResolver.class, new FileResolver(resourceManager));
		
		return this;
	}
	
	public ExecutionContext build() {
		return executionContext;
	}
}
