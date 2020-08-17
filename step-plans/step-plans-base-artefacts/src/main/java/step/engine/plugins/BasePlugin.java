package step.engine.plugins;

import java.util.HashMap;
import java.util.Map;

import step.artefacts.handlers.DefaultFunctionRouterImpl;
import step.artefacts.handlers.FunctionRouter;
import step.attachments.FileResolver;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.model.ExecutionParameters;
import step.core.plugins.Plugin;
import step.core.variables.VariableType;
import step.core.variables.VariablesManager;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.execution.FunctionExecutionServiceImpl;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.client.GridClient;
import step.grid.client.MockedGridClientImpl;

@Plugin(dependencies= {ResourceManagerPlugin.class})
public class BasePlugin extends AbstractExecutionEnginePlugin {

	private FunctionAccessor functionAccessor;
	private GridClient gridClient;
	private FunctionTypeRegistry functionTypeRegistry;
	private FunctionRouter functionRouter;
	private FunctionExecutionService functionExecutionService;

	@Override
	public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
		functionAccessor = context.inheritFromParentOrComputeIfAbsent(parentContext, FunctionAccessor.class, k->new InMemoryFunctionAccessorImpl());
		gridClient = context.inheritFromParentOrComputeIfAbsent(parentContext, GridClient.class ,k->new MockedGridClientImpl());
		FileResolver fileResolver = context.require(FileResolver.class);
		functionTypeRegistry = context.inheritFromParentOrComputeIfAbsent(parentContext, FunctionTypeRegistry.class, k->new FunctionTypeRegistryImpl(fileResolver, gridClient));
		
		functionExecutionService = context.computeIfAbsent(FunctionExecutionService.class, k->{
			try {
				return new FunctionExecutionServiceImpl(gridClient, functionTypeRegistry, context.getDynamicBeanResolver());
			} catch (FunctionExecutionServiceException e) {
				throw new RuntimeException(e);
			}
		});
		
		functionRouter = context.computeIfAbsent(FunctionRouter.class, k->{
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
			context.put(FunctionAccessor.class, functionAccessor);
			context.put(FunctionExecutionService.class, functionExecutionService);
			context.put(FunctionRouter.class, functionRouter);
			context.put(FunctionTypeRegistry.class, functionTypeRegistry);
		} else {
			context.put(FunctionAccessor.class, functionAccessor);
			context.put(FunctionExecutionService.class, functionExecutionService);
			context.put(FunctionRouter.class, functionRouter);
			context.put(FunctionTypeRegistry.class, functionTypeRegistry);
		}
	}

	@Override
	public void executionStart(ExecutionContext context) {
		super.executionStart(context);
		ReportNode rootNode = context.getReport();
		// Create the contextual global parameters 
		Map<String, String> globalParametersFromExecutionParameters = new HashMap<>();
		ExecutionParameters executionParameters = context.getExecutionParameters();
		if(executionParameters.getUserID() != null) {
			globalParametersFromExecutionParameters.put("user", executionParameters.getUserID());
		}
		if(executionParameters.getCustomParameters() != null) {
			globalParametersFromExecutionParameters.putAll(executionParameters.getCustomParameters());			
		}
		VariablesManager variablesManager = context.getVariablesManager();
		globalParametersFromExecutionParameters.forEach((k,v)->variablesManager.putVariable(rootNode, VariableType.IMMUTABLE, k, v));
	}
}
