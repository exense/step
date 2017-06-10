package step.functions.type;

import java.util.Map;

import step.core.GlobalContext;
import step.functions.Function;

public abstract class AbstractFunctionType<T extends Function> {

	protected GlobalContext context;
	
	public GlobalContext getContext() {
		return context;
	}

	public void setContext(GlobalContext context) {
		this.context = context;
	}
	
	public void init() {}

	public abstract String getHandlerChain(T function);
	
	public abstract Map<String, String> getHandlerProperties(T function);
	
	public abstract T newFunction();
	
	public void setupFunction(T function) throws SetupFunctionException {
		
	}
	
	public T copyFunction(T function) {
		function.setId(null);
		function.getAttributes().put(Function.NAME,function.getAttributes().get(Function.NAME)+"_Copy");
		return function;
	}
}
