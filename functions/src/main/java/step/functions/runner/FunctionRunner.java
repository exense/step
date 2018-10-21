package step.functions.runner;

import java.io.StringReader;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import step.attachments.AttachmentManager;
import step.attachments.FileResolver;
import step.commons.conf.Configuration;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.Input;
import step.functions.Output;
import step.functions.accessor.FunctionCRUDAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceImpl;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.Grid;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.client.GridClientImpl;

public class FunctionRunner {

	public static class Context {
				
		AgentTokenWrapper token;
		
		FunctionExecutionService functionExecutionService;
		
		FunctionCRUDAccessor functionAccessor = new InMemoryFunctionAccessorImpl();

		protected Context(Configuration configuration, AbstractFunctionType<?> functionType, Map<String, String> properties) {
			super();
			token = new AgentTokenWrapper();
			if(properties!=null) {
				token.setProperties(properties);
			}
			Grid grid = new Grid(0);
			GridClientImpl client = new GridClientImpl(grid, grid);
			
			FunctionTypeRegistry functionTypeRegistry = new FunctionTypeRegistryImpl(new FileResolver(new AttachmentManager(configuration)), grid);
			functionTypeRegistry.registerFunctionType(functionType);
			
			functionExecutionService = new FunctionExecutionServiceImpl(client, functionAccessor, functionTypeRegistry, new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler())));
		} 
		
		private JsonObject read(String argument) {
			return Json.createReader(new StringReader(argument)).readObject();
		}
		
		public Output run(Function function, String argument, Map<String, String> properties) {	
			return run(function, read(argument), properties);
		}
		
		public Output run(Function function, JsonObject argument, Map<String, String> properties) {	
			functionAccessor.save(function);
			
			Input input = new Input();
			input.setArgument(argument);
			input.setProperties(properties);
			
			try {
				return functionExecutionService.callFunction(functionExecutionService.getLocalTokenHandle(), function.getId().toString(), input);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public static Context getContext(AbstractFunctionType<?> functionType) {
		return new Context(new Configuration(),functionType, null);
	}
	
	public static Context getContext(AbstractFunctionType<?> functionType, Map<String, String> properties) {
		return new Context(new Configuration(),functionType, properties);
	}
	
	public static Context getContext(Configuration configuration,AbstractFunctionType<?> functionType, Map<String, String> properties) {
		return new Context(configuration,functionType, properties);
	}
	
}
