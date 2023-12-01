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
package step.engine.plugins;

import org.bson.types.ObjectId;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.plugins.Plugin;
import step.core.scanner.CachedAnnotationScanner;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.handlers.javahandler.Keyword;
import step.handlers.javahandler.KeywordExecutor;
import step.plugins.java.handler.KeywordHandler;

import java.lang.reflect.Method;
import java.util.*;

@Plugin(dependencies= {FunctionPlugin.class})
public class LocalFunctionPlugin extends AbstractExecutionEnginePlugin {

	private FunctionAccessor functionAccessor;
	private FunctionTypeRegistry functionTypeRegistry;

	@Override
	public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
		if(context.getOperationMode() == OperationMode.LOCAL) {
			functionAccessor = context.require(FunctionAccessor.class);
			functionTypeRegistry = context.require(FunctionTypeRegistry.class);
			
			functionTypeRegistry.registerFunctionType(new LocalFunctionType());
			List<Function> localFunctions = getLocalFunctions();
			functionAccessor.save(localFunctions);
		}
	}

	public List<Function> getLocalFunctions() {
		Set<Method> methods = CachedAnnotationScanner.getMethodsWithAnnotation(Keyword.class);
		List<Function> functions = new ArrayList<Function>();
		for (Method m : methods) {
			Keyword annotation = m.getAnnotation(Keyword.class);

			// keywords with plan reference are not local functions but composite functions linked with plan
			if (annotation.planReference() == null || annotation.planReference().isBlank()) {
				LocalFunction function = createLocalFunction(m, annotation);

				functions.add(function);
			}
		}
		return functions;
	}

	public static LocalFunction createLocalFunction(Method m, Keyword annotation) {
		String functionName = annotation.name().length() > 0 ? annotation.name() : m.getName();

		LocalFunction function = new LocalFunction();
		function.getCallTimeout().setValue(annotation.timeout());
		function.setAttributes(new HashMap<>());
		function.getAttributes().put(AbstractOrganizableObject.NAME, functionName);
		function.setClassName(m.getDeclaringClass().getName());
		return function;
	}

	public static class LocalFunction extends Function {
		
		String className;
		
		public LocalFunction() {
			super();
			this.setId(new ObjectId());
		}
	
		@Override
		public boolean requiresLocalExecution() {
			return true;
		}

		public String getClassName() {
			return className;
		}

		public void setClassName(String className) {
			this.className = className;
		}
	}
	
	private class LocalFunctionType extends AbstractFunctionType<LocalFunction> {

		@Override
		public String getHandlerChain(LocalFunction function) {
			return KeywordHandler.class.getName();
		}

		@Override
		public Map<String, String> getHandlerProperties(LocalFunction function) {
			Map<String, String> properties = new HashMap<>();
			
			StringBuilder classes = new StringBuilder();
			classes.append(function.getClassName()+";");
			properties.put(KeywordExecutor.KEYWORD_CLASSES, classes.toString());
			
			return properties;
		}

		@Override
		public LocalFunction newFunction() {
			return new LocalFunction();
		}
	}
}
