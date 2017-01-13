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

	public abstract String getHandlerChain(T functionTypeConf);
	
	public abstract Map<String, String> getHandlerProperties(T functionTypeConf);
	
	public abstract T newFunctionTypeConf();
	
	public void setupFunction(Function function) {
		
	}
	
	public Function copyFunction(Function function) {
		function.setId(null);
		function.getAttributes().put("name",function.getAttributes().get("name")+"_Copy");
		return function;
	}
	
	public String getEditorPath(Function function) {
		return null;
	}
}
