package step.functions.type;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import step.core.GlobalContext;
import step.functions.Function;
import step.grid.Grid;
import step.grid.tokenpool.Interest;

public abstract class AbstractFunctionType<T extends Function> {

	protected GlobalContext context;
	
	public GlobalContext getContext() {
		return context;
	}

	public void setContext(GlobalContext context) {
		this.context = context;
	}
	
	public void init() {}

	public Map<String, Interest> getTokenSelectionCriteria(T function) {
		Map<String, Interest> criteria = new HashMap<>();
		criteria.put(Grid.AGENT_TYPE_KEY, new Interest(Pattern.compile("default"), true));
		return criteria;
	}
	
	public abstract String getHandlerChain(T function);
	
	public abstract Map<String, String> getHandlerProperties(T function);
	
	public abstract T newFunction();
	
	public void setupFunction(T function) throws SetupFunctionException {
		
	}
	
	public T updateFunction(T function) {
		return function;
	}
	
	public T copyFunction(T function) {
		function.setId(null);
		function.getAttributes().put(Function.NAME,function.getAttributes().get(Function.NAME)+"_Copy");
		return function;
	}
	
	public void deleteFunction(T function) {

	}
}
