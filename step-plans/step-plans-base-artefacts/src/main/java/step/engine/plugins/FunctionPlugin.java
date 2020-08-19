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
	private FunctionManager functionManager;

	@Override
	public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
		FileResolver fileResolver = context.getFileResolver();

		functionAccessor = context.inheritFromParentOrComputeIfAbsent(parentContext, FunctionAccessor.class, k->new InMemoryFunctionAccessorImpl());
		functionTypeRegistry = context.inheritFromParentOrComputeIfAbsent(parentContext, FunctionTypeRegistry.class, k->new FunctionTypeRegistryImpl(fileResolver, gridClient));
		functionManager = context.inheritFromParentOrComputeIfAbsent(parentContext, FunctionManager.class, k->new FunctionManagerImpl(functionAccessor, functionTypeRegistry));
		
		gridClient = context.inheritFromParentOrComputeIfAbsent(parentContext, GridClient.class ,k->new MockedGridClientImpl());
		
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
		boolean isolatedExecution = context.getExecutionParameters().isIsolatedExecution();
		if(isolatedExecution) {
			FunctionAccessor functionAccessor = new InMemoryFunctionAccessorImpl();
			FunctionExecutionService functionExecutionService;
			try {
				functionExecutionService = new FunctionExecutionServiceImpl(gridClient, functionTypeRegistry, context.getDynamicBeanResolver());
			} catch (FunctionExecutionServiceException e) {
				throw new RuntimeException("Error while creating function execution service", e);
			}
			DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
			FunctionRouter functionRouter = new DefaultFunctionRouterImpl(functionExecutionService, functionTypeRegistry, dynamicJsonObjectResolver);
			
			FunctionManagerImpl functionManager = new FunctionManagerImpl(functionAccessor, functionTypeRegistry);
			context.put(FunctionAccessor.class, functionAccessor);
			context.put(FunctionManager.class, functionManager);
			context.put(FunctionTypeRegistry.class, functionTypeRegistry);
			context.put(FunctionExecutionService.class, functionExecutionService);
			context.put(FunctionRouter.class, functionRouter);
		} else {
			context.put(FunctionAccessor.class, functionAccessor);
			context.put(FunctionManager.class, functionManager);
			context.put(FunctionTypeRegistry.class, functionTypeRegistry);
			context.put(FunctionExecutionService.class, functionExecutionService);
			context.put(FunctionRouter.class, functionRouter);
		}
	}
}
