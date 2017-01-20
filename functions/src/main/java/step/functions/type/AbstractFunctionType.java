package step.functions.type;

import java.util.Map;

import step.core.GlobalContext;
import step.functions.Function;

public abstract class AbstractFunctionType<T extends FunctionTypeConf> {

	protected GlobalContext context;
	
	public GlobalContext getContext() {
		return context;
	}

	public void setContext(GlobalContext context) {
		this.context = context;
	}
	
	public void init() {}

	public abstract String getHandlerChain(Function function);
	
	public abstract Map<String, String> getHandlerProperties(Function function);
	
	public abstract T newFunctionTypeConf();
	
	public void setupFunction(Function function) throws SetupFunctionException {
		
	}
	
	public Function copyFunction(Function function) {
		function.setId(null);
		function.getAttributes().put("name",function.getAttributes().get("name")+"_Copy");
		return function;
	}
	
	@SuppressWarnings("unchecked")
	protected T getFunctionConf(Function function) {
		return (T) function.getConfiguration();
	}
}
