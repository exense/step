package step.localrunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;

import org.bson.types.ObjectId;

import ch.exense.commons.app.Configuration;
import step.artefacts.handlers.DefaultFunctionRouterImpl;
import step.artefacts.handlers.FunctionRouter;
import step.attachments.FileResolver;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.execution.ExecutionContext;
import step.core.plans.runner.DefaultPlanRunner;
import step.core.plans.runner.PlanRunner;
import step.core.variables.VariableType;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.FunctionCRUDAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.execution.FunctionExecutionServiceImpl;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.client.GridClient;
import step.grid.client.MockedGridClientImpl;
import step.handlers.javahandler.KeywordExecutor;
import step.plugins.functions.types.CompositeFunctionType;
import step.plugins.java.handler.KeywordHandler;
import step.resources.LocalResourceManagerImpl;

/**
 * A runner that runs plans and functions locally.
 * The list of classes containing functions has to be passed to the constructor
 * 
 * @author Jérôme Comte
 *
 */
public class LocalPlanRunner extends DefaultPlanRunner implements PlanRunner {
	
	protected List<Class<?>> functionClasses;
	protected Map<String, String> properties;
	
	/**
	 * @param functionClasses functionClasses the list of Classes containing the functions (aka Keywords)
	 */
	public LocalPlanRunner(Class<?>... functionClasses) {
		this(null, Arrays.asList(functionClasses));
	}
	
	/**
	 * @param functionClasses functionClasses the list of Classes containing the functions (aka Keywords)
	 */
	public LocalPlanRunner(List<Class<?>> functionClasses) {
		this(null, functionClasses);
	}
	
	/**
	 * @param properties a map containing the properties that are usually set under Parameters in the UI
	 * @param functionClasses the list of Classes containing the functions (aka Keywords)
	 */
	public LocalPlanRunner(Map<String, String> properties, Class<?>... functionClasses) {
		this(properties, Arrays.asList(functionClasses));
	}
	
	/**
	 * @param properties a map containing the properties that are usually set under Parameters in the UI
	 * @param functionClasses the list of Classes containing the functions (aka Keywords)
	 */
	public LocalPlanRunner(Map<String, String> properties, List<Class<?>> functionClasses) {
		super();
		this.properties = properties;
		this.functionClasses = functionClasses;
	}

	public static class LocalFunction extends Function {

		public LocalFunction() {
			super();
			this.setId(new ObjectId());
		}

		@Override
		public boolean requiresLocalExecution() {
			return true;
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
			functionClasses.forEach(cl->{classes.append(cl.getName()+";");});
			properties.put(KeywordExecutor.KEYWORD_CLASSES, classes.toString());
			
			return properties;
		}

		@Override
		public LocalFunction newFunction() {
			return new LocalFunction();
		}
		
	}
	
	@Override
	protected ExecutionContext buildExecutionContext() {
		ExecutionContext context = super.buildExecutionContext();
		
		GridClient gridClient = new MockedGridClientImpl();
		
		InMemoryFunctionAccessorImpl inMemoryFunctionAccessor = new InMemoryFunctionAccessorImpl();		
		FunctionAccessor functionAccessor = new FunctionCRUDAccessor() {
			 
			@Override
			public Function get(ObjectId id) {
				return inMemoryFunctionAccessor.get(id);
			}
			
			@Override
			public Function findByAttributes(Map<String, String> attributes) {
				Function function = inMemoryFunctionAccessor.findByAttributes(attributes);
				function = defaultLocalFunctionIfNull(inMemoryFunctionAccessor, attributes, function);
				return function;
			}

			protected Function defaultLocalFunctionIfNull(InMemoryFunctionAccessorImpl inMemoryFunctionAccessor,
					Map<String, String> attributes, Function function) {
				if(function == null) {
					function = defaultLocalFunction(inMemoryFunctionAccessor, attributes);
				}
				return function;
			}

			protected Function defaultLocalFunction(InMemoryFunctionAccessorImpl inMemoryFunctionAccessor,
					Map<String, String> attributes) {
				// Use a local function per default
				Function function = new LocalFunction();
				function.setAttributes(attributes);
				inMemoryFunctionAccessor.save(function);
				return function;
			}

			@Override
			public void remove(ObjectId id) {
				inMemoryFunctionAccessor.remove(id);
			}

			@Override
			public Function save(Function entity) {
				return inMemoryFunctionAccessor.save(entity);
			}

			@Override
			public void save(Collection<? extends Function> entities) {
				inMemoryFunctionAccessor.save(entities);
			}

			@Override
			public Iterator<Function> getAll() {
				return inMemoryFunctionAccessor.getAll();
			}

			@Override
			public Spliterator<Function> findManyByAttributes(Map<String, String> attributes) {
				List<Function> result = new ArrayList<>();
				inMemoryFunctionAccessor.findManyByAttributes(attributes).forEachRemaining(f->result.add(defaultLocalFunctionIfNull(inMemoryFunctionAccessor, attributes, f)));
				if(result.size() == 0) {
					result.add(defaultLocalFunction(inMemoryFunctionAccessor, attributes));
				}
				return result.spliterator();
			}

			@Override
			public Function findByAttributes(Map<String, String> attributes, String attributesMapKey) {
				throw new UnsupportedOperationException("This method is currently not implemented");
			}

			@Override
			public Spliterator<Function> findManyByAttributes(Map<String, String> attributes, String attributesMapKey) {
				throw new UnsupportedOperationException("This method is currently not implemented");
			}

			@Override
			public Function get(String id) {
				return get(new ObjectId(id));
			}

		};	
		
		Configuration configuration = new Configuration();
		
		LocalFunctionType type = new LocalFunctionType();
		FunctionTypeRegistry functionTypeRegistry = new FunctionTypeRegistryImpl(new FileResolver(new LocalResourceManagerImpl()), gridClient);
		functionTypeRegistry.registerFunctionType(type);
		
		CompositeFunctionType compositeFunctionType = new CompositeFunctionType(context.getPlanAccessor());
		functionTypeRegistry.registerFunctionType(compositeFunctionType);
		
		FunctionExecutionService functionExecutionService;
		try {
			functionExecutionService = new FunctionExecutionServiceImpl(gridClient, functionTypeRegistry, new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler())));
		} catch (FunctionExecutionServiceException e) {
			throw new RuntimeException(e);
		}

		context.put(FunctionExecutionService.class, functionExecutionService);
		context.put(FunctionAccessor.class, functionAccessor);
		context.put(FunctionRouter.class, new DefaultFunctionRouterImpl(functionExecutionService, functionTypeRegistry, new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()))));
		
		if(properties != null) {
			for(String key:properties.keySet()) {
				context.getVariablesManager().putVariable(context.getReport(), VariableType.IMMUTABLE, key, properties.get(key));
			}
		}
		
		return context;
	}
}
