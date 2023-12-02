/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.functions.runner;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import ch.exense.commons.app.Configuration;
import ch.exense.commons.io.FileHelper;
import step.attachments.FileResolver;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.execution.FunctionExecutionServiceImpl;
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.client.GridClient;
import step.grid.client.MockedGridClientImpl;
import step.resources.LocalResourceManagerImpl;

public class FunctionRunner {

	public static class Context implements Closeable {
				
		Map<String, String> properties;
		
		FunctionExecutionService functionExecutionService;
		
		GridClient client;
		File fileManagerDirectory;
		
		protected Context(Configuration configuration, AbstractFunctionType<?> functionType, Map<String, String> properties) {
			super();

			this.properties = properties;
			try {
				this.fileManagerDirectory = FileHelper.createTempFolder();
			} catch (IOException e1) {
				throw new RuntimeException("Error while creating file manager directory", e1);
			}
			
			client = new MockedGridClientImpl();
			
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
			FunctionInput<JsonObject> input = new FunctionInput<>();
			input.setPayload(argument);
			input.setProperties(properties);
			
			try {
				return functionExecutionService.callFunction(functionExecutionService.getLocalTokenHandle().getID(), function, input, JsonObject.class, null);
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
