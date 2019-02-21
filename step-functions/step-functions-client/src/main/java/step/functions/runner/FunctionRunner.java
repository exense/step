package step.functions.runner;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import step.attachments.FileResolver;
import step.commons.conf.Configuration;
import step.commons.helpers.FileHelper;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.execution.FunctionExecutionServiceImpl;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.Grid;
import step.grid.client.GridClientImpl;
import step.resources.LocalResourceManagerImpl;

public class FunctionRunner {

	public static class Context implements Closeable {
				
		Map<String, String> properties;
		
		FunctionExecutionService functionExecutionService;
		
		Grid grid;
		GridClientImpl client;
		File fileManagerDirectory;
		
		protected Context(Configuration configuration, AbstractFunctionType<?> functionType, Map<String, String> properties) {
			super();

			this.properties = properties;
			this.fileManagerDirectory = FileHelper.createTempFolder();
			
			grid = new Grid(fileManagerDirectory, 0);
			
			client = new GridClientImpl(grid);
			
			FileResolver fileResolver = new FileResolver(new LocalResourceManagerImpl());
			FunctionTypeRegistry functionTypeRegistry = new FunctionTypeRegistryImpl(fileResolver, client);
			functionTypeRegistry.registerFunctionType(functionType);
			
			try {
				functionExecutionService = new FunctionExecutionServiceImpl(client, functionTypeRegistry, new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler())));
			} catch (FunctionExecutionServiceException e) {
				throw new RuntimeException("Error while creating function execution service", e);
			}
		} 
		
		private JsonObject read(String argument) {
			return Json.createReader(new StringReader(argument)).readObject();
		}
		
		public Output<JsonObject> run(Function function, String argument) {	
			return run(function, read(argument));
		}
		
		public Output<JsonObject> run(Function function, JsonObject argument) {	
			Input<JsonObject> input = new Input<>();
			input.setPayload(argument);
			input.setProperties(properties);
			
			try {
				return functionExecutionService.callFunction(functionExecutionService.getLocalTokenHandle(), function, input, JsonObject.class);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void close() throws IOException {
			client.close();
			FileHelper.deleteFolder(fileManagerDirectory);
		}
	}
	
	public static Context getContext(AbstractFunctionType<?> functionType) {
		return new Context(new Configuration(),functionType, new HashMap<>());
	}
	
	public static Context getContext(AbstractFunctionType<?> functionType, Map<String, String> properties) {
		return new Context(new Configuration(),functionType, properties);
	}
	
	public static Context getContext(Configuration configuration,AbstractFunctionType<?> functionType, Map<String, String> properties) {
		return new Context(configuration,functionType, properties);
	}
	
}
