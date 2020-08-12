package step.engine.plugins;

import java.util.HashMap;
import java.util.Map;

import step.artefacts.handlers.DefaultFunctionRouterImpl;
import step.artefacts.handlers.FunctionRouter;
import step.attachments.FileResolver;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
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

	@Override
	public void initialize(ExecutionEngineContext context) {
		FileResolver fileResolver = context.get(FileResolver.class);
		context.computeIfAbsent(FunctionAccessor.class,k->new InMemoryFunctionAccessorImpl());
		GridClient gridClient = context.computeIfAbsent(GridClient.class,k->new MockedGridClientImpl());
		
		FunctionTypeRegistry functionTypeRegistry = context.computeIfAbsent(FunctionTypeRegistry.class, k->new FunctionTypeRegistryImpl(fileResolver, gridClient));
		
		FunctionExecutionService functionExecutionService = context.computeIfAbsent(FunctionExecutionService.class, k->{
			try {
				return new FunctionExecutionServiceImpl(gridClient, functionTypeRegistry, context.getDynamicBeanResolver());
			} catch (FunctionExecutionServiceException e) {
				throw new RuntimeException(e);
			}
		});
		
		context.computeIfAbsent(FunctionRouter.class, k->{
			DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
			return new DefaultFunctionRouterImpl(functionExecutionService, functionTypeRegistry, dynamicJsonObjectResolver);
		});
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
