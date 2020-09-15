package step.engine.plugins;

import step.artefacts.handlers.DefaultFunctionRouterImpl;
import step.artefacts.handlers.FunctionRouter;
import step.attachments.FileResolver;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.Plugin;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.accessor.LayeredFunctionAccessor;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.execution.FunctionExecutionServiceImpl;
import step.functions.manager.FunctionManager;
import step.functions.manager.FunctionManagerImpl;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.client.GridClient;
import step.grid.client.MockedGridClientImpl;

@Plugin(dependencies= {})
public class FunctionPlugin extends AbstractExecutionEnginePlugin {

	private FunctionAccessor functionAccessor;
	private GridClient gridClient;
	private FunctionTypeRegistry functionTypeRegistry;
	private FunctionRouter functionRouter;
	private FunctionExecutionService functionExecutionService;

	@Override
	public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
		FileResolver fileResolver = context.getFileResolver();

		gridClient = context.inheritFromParentOrComputeIfAbsent(parentContext, GridClient.class ,k->new MockedGridClientImpl());

		functionAccessor = context.inheritFromParentOrComputeIfAbsent(parentContext, FunctionAccessor.class, k->new InMemoryFunctionAccessorImpl());
		functionTypeRegistry = context.inheritFromParentOrComputeIfAbsent(parentContext, FunctionTypeRegistry.class, k->new FunctionTypeRegistryImpl(fileResolver, gridClient));
		
		functionExecutionService = context.inheritFromParentOrComputeIfAbsent(parentContext, FunctionExecutionService.class, k->{
			try {
				return new FunctionExecutionServiceImpl(gridClient, functionTypeRegistry, context.getDynamicBeanResolver());
			} catch (FunctionExecutionServiceException e) {
				throw new RuntimeException(e);
			}
		});
		
		functionRouter = context.inheritFromParentOrComputeIfAbsent(parentContext, FunctionRouter.class, k->{
			DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
			return new DefaultFunctionRouterImpl(functionExecutionService, functionTypeRegistry, dynamicJsonObjectResolver);
		});
	}

	@Override
	public void initializeExecutionContext(ExecutionEngineContext executionEngineContext,
			ExecutionContext context) {
		// Use a layered function accessor to isolate the local context from the parent one
		// This allow temporary persistence of function for the duration of the execution
		LayeredFunctionAccessor layeredFunctionAccessor = new LayeredFunctionAccessor();
		layeredFunctionAccessor.pushAccessor(functionAccessor);
		layeredFunctionAccessor.pushAccessor(new InMemoryFunctionAccessorImpl());
		FunctionManagerImpl functionManager = new FunctionManagerImpl(layeredFunctionAccessor, functionTypeRegistry);
		
		context.put(FunctionAccessor.class, layeredFunctionAccessor);
		context.put(FunctionManager.class, functionManager);
		context.put(FunctionTypeRegistry.class, functionTypeRegistry);
		context.put(FunctionExecutionService.class, functionExecutionService);
		context.put(FunctionRouter.class, functionRouter);
	}
}
