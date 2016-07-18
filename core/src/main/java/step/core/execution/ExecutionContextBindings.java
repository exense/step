package step.core.execution;

import java.util.HashMap;
import java.util.Map;

public class ExecutionContextBindings {

	public static Map<String, Object> get(ExecutionContext context) {
		Map<String, Object> bindings = new HashMap<String, Object>();
		bindings.putAll(context.getVariablesManager().getAllVariables());
		return bindings;
	}
	
}

