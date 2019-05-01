package step.core.execution;

import org.bson.types.ObjectId;

import step.core.GlobalContext;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.threadpool.ThreadPool;

public class ControllerSideExecutionContextBuilder {

	public static ExecutionContext createExecutionContext(GlobalContext globalContext) {
		ExecutionContext context = new ExecutionContext(new ObjectId().toString());
		context.setExecutionParameters(new ExecutionParameters("dummy", null, ExecutionMode.RUN));
		context.setExpressionHandler(globalContext.getExpressionHandler());
		context.setDynamicBeanResolver(globalContext.getDynamicBeanResolver());
		context.setConfiguration(globalContext.getConfiguration());
		context.setArtefactAccessor(globalContext.getArtefactAccessor());
		context.setReportNodeAccessor(globalContext.getReportAccessor());
		context.setEventManager(globalContext.getEventManager());
		context.setExecutionCallbacks(globalContext.getPluginManager().getProxy());
		context.put(ThreadPool.class, new ThreadPool(context));
		context.setExecutionTypeListener(new ExecutionTypeListener() {
			@Override
			public void updateExecutionType(ExecutionContext context, String newType) {
			}
		});
		return context;
	}
}
