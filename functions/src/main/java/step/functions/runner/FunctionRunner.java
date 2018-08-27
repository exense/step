package step.functions.runner;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import step.attachments.AttachmentManager;
import step.commons.conf.Configuration;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.FunctionClient;
import step.functions.FunctionRepository;
import step.functions.Input;
import step.functions.Output;
import step.functions.type.AbstractFunctionType;
import step.grid.Grid;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.client.GridClient;

public class FunctionRunner {

	public static class Context {
				
		AgentTokenWrapper token;
		
		FunctionClient functionClient;
		
		FunctionRepository functionRepository = new FunctionRepository() {
			
			Map<String, Function> functions = new HashMap<>();
			
			@Override
			public Function getFunctionById(String id) {
				return functions.get(id);
			}
			
			@Override
			public Function getFunctionByAttributes(Map<String, String> attributes) {
				throw new RuntimeException();
			}
			
			@Override
			public void deleteFunction(String functionId) {
				
			}
			
			@Override
			public void addFunction(Function function) {
				functions.put(function.getId().toString(), function);
			}
		};

		protected Context(Configuration configuration, AbstractFunctionType<?> functionType, Map<String, String> properties) {
			super();
			token = new AgentTokenWrapper();
			if(properties!=null) {
				token.setProperties(properties);
			}
			Grid grid = new Grid(0);
			GridClient client = new GridClient(grid, grid);
			functionClient = new FunctionClient(new AttachmentManager(configuration), configuration, new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler())), client, functionRepository);
			functionClient.registerFunctionType(functionType);
			
		} 
		
		private JsonObject read(String argument) {
			return Json.createReader(new StringReader(argument)).readObject();
		}
		
		public Output run(Function function, String argument, Map<String, String> properties) {	
			return run(function, read(argument), properties);
		}
		
		public Output run(Function function, JsonObject argument, Map<String, String> properties) {	
			functionRepository.addFunction(function);
			
			Input input = new Input();
			input.setArgument(argument);
			input.setProperties(properties);
			
			try {
				return functionClient.callFunction(functionClient.getLocalTokenHandle(), function.getId().toString(), input);
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
