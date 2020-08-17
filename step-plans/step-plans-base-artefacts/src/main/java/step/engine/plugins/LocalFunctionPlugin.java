package step.engine.plugins;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Attribute;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.plugins.Plugin;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.FunctionCRUDAccessor;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.handlers.javahandler.Keyword;
import step.handlers.javahandler.KeywordExecutor;
import step.plugins.java.handler.KeywordHandler;

@Plugin(dependencies= {BasePlugin.class})
public class LocalFunctionPlugin extends AbstractExecutionEnginePlugin {

	private FunctionCRUDAccessor functionAccessor;
	private FunctionTypeRegistry functionTypeRegistry;

	@Override
	public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
		if(context.getOperationMode() == OperationMode.LOCAL) {
			functionAccessor = (FunctionCRUDAccessor) context.require(FunctionAccessor.class);
			functionTypeRegistry = context.require(FunctionTypeRegistry.class);
			
			functionTypeRegistry.registerFunctionType(new LocalFunctionType());
			List<Function> localFunctions = getLocalFunctions();
			functionAccessor.save(localFunctions);
		}
	}
	
	public List<Function> getLocalFunctions() {
		List<Function> functions = new ArrayList<Function>();
		// TODO migrate Refelections to io.github.classgraph
		Reflections reflections = new Reflections(new ConfigurationBuilder().forPackages("step")
				.setScanners(new MethodAnnotationsScanner()));
		
		Set<Method> methods;
		try {
			methods = reflections.getMethodsAnnotatedWith(Keyword.class);
		} catch(Throwable e) {
			throw new RuntimeException(e);
		}
		for(Method m:methods) {
			Keyword annotation = m.getAnnotation(Keyword.class);
			
			String functionName = annotation.name().length()>0?annotation.name():m.getName();
			
			LocalFunction function = new LocalFunction();
			function.setAttributes(new HashMap<>());
			function.getAttributes().put(AbstractOrganizableObject.NAME, functionName);
			function.setClassName(m.getDeclaringClass().getName());

			List<Attribute> attributes = new ArrayList<>();
			attributes.addAll(Arrays.asList(m.getDeclaringClass().getAnnotationsByType(Attribute.class)));
			attributes.addAll(Arrays.asList(m.getAnnotationsByType(Attribute.class)));
			for (Attribute attribute : attributes) {
				function.getAttributes().put(attribute.key(), attribute.value());
			}
			
			functions.add(function);
		}
		return functions;
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
