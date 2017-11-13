package step.functions.runner;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import step.core.GlobalContext;
import step.core.execution.ContextBuilder;
import step.functions.Function;
import step.functions.FunctionClient;
import step.functions.FunctionRepository;
import step.functions.Input;
import step.functions.Output;
import step.functions.type.AbstractFunctionType;
import step.grid.Grid;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.client.GridClient;
import step.plugins.adaptergrid.GridPlugin;

public class FunctionRunner {

	public static class Context {
				
		GlobalContext globalContext;
		
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

		public Context(AbstractFunctionType<?> functionType, Map<String, String> properties) {
			super();
			token = new AgentTokenWrapper();
			if(properties!=null) {
				token.setProperties(properties);
			}
			globalContext = ContextBuilder.createGlobalContext();
			Grid grid = new Grid(0);
			GridClient client = new GridClient(grid, grid);
			functionClient = new FunctionClient(globalContext, client, functionRepository);
			functionClient.registerFunctionType(functionType);
			globalContext.put(GridPlugin.FUNCTIONCLIENT_KEY, functionClient);
			
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

		public GlobalContext getGlobalContext() {
			return globalContext;
		}
	}
	
	public static Context getContext(AbstractFunctionType<?> functionType) {
		return new Context(functionType, null);
	}
	
	public static Context getContext(AbstractFunctionType<?> functionType, Map<String, String> properties) {
		return new Context(functionType, properties);
	}
	
}
