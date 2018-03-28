package step.functions.runner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;

import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ContextBuilder;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.core.plans.runner.PlanRunner;
import step.functions.Function;
import step.functions.FunctionClient;
import step.functions.FunctionExecutionService;
import step.functions.FunctionRepository;
import step.functions.routing.FunctionRouter;
import step.functions.type.AbstractFunctionType;
import step.grid.agent.handler.MessageHandler;
import step.grid.client.GridClient;
import step.grid.filemanager.FileManagerClient.FileVersionId;
import step.plugins.adaptergrid.GridPlugin;

public class LocalRunner implements PlanRunner {
	
	ExecutionContext context;
	
	MessageHandler handler;

	public LocalRunner(MessageHandler handler) {
		super();
		this.handler = handler;
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
	
	public static class LocalFunctionType extends AbstractFunctionType<LocalFunction> {

		final MessageHandler handler;
		
		public LocalFunctionType(MessageHandler handler) {
			super();
			this.handler = handler;
		}

		@Override
		public FileVersionId getHandlerPackage(LocalFunction function) {
			// TODO Auto-generated method stub
			return super.getHandlerPackage(function);
		}

		@Override
		public String getHandlerChain(LocalFunction function) {
			return handler.getClass().getName();
		}

		@Override
		public Map<String, String> getHandlerProperties(LocalFunction function) {
			return null;
		}

		@Override
		public LocalFunction newFunction() {
			return new LocalFunction();
		}
		
	}
	
	protected void init() {
		context = ContextBuilder.createLocalExecutionContext();
		
		GridClient gridClient = new GridClient(null, null);
		
		FunctionRepository functionRepo = new FunctionRepository() {
			
			Map<ObjectId,Function> functions = new HashMap<>();
			
			@Override
			public Function getFunctionById(String id) {
				return functions.get(new ObjectId(id));
			}
			
			@Override
			public Function getFunctionByAttributes(Map<String, String> attributes) {
				LocalFunction f = new LocalFunction();
				f.setAttributes(attributes);
				functions.put(f.getId(), f);
				return f;
			}
			
			@Override
			public void deleteFunction(String functionId) {
			}
			
			@Override
			public void addFunction(Function function) {
			}
		};		
		
		FunctionClient functionClient = new FunctionClient(context.getGlobalContext(), gridClient, functionRepo);
		LocalFunctionType type = new LocalFunctionType(handler);
		functionClient.registerFunctionType(type);

		context.getGlobalContext().put(GridPlugin.FUNCTIONCLIENT_KEY, functionClient);
		context.getGlobalContext().put(FunctionExecutionService.class, functionClient);
		context.getGlobalContext().put(FunctionRepository.class, functionRepo);
		context.getGlobalContext().put(FunctionRouter.class, new FunctionRouter(functionClient, new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getGlobalContext().getExpressionHandler()))));
	}

	public ExecutionContext getContext() {
		return context;
	}

	@Override
	public ReportNode run(Plan plan) {
		init();
		context.getGlobalContext().getArtefactAccessor().save(new ArrayList<>(plan.getArtefacts()));
		return ArtefactHandler.delegateExecute(context, plan.getRoot(),context.getReport());
	}
}
