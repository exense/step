package step.core.artefacts.handlers;

import java.util.HashMap;
import java.util.Map;

import step.core.execution.ExecutionContext;
import step.core.variables.VariablesManager;

public class ReportNodeAttributesManager {

	private static String CUSTOM_ATTRIBUTES_PREFIX = "#customAttributes#";
	
	public static void addCustomAttribute(String key, String value) {
		VariablesManager varMan = ExecutionContext.getCurrentContext().getVariablesManager();
		varMan.putVariable(ExecutionContext.getCurrentReportNode(), CUSTOM_ATTRIBUTES_PREFIX+key, value);
	}
	
	public static Map<String, String> getCustomAttributes() {
		Map<String,String> result = new HashMap<>();
		VariablesManager varMan = ExecutionContext.getCurrentContext().getVariablesManager();
		Map<String, Object> allVars = varMan.getAllVariables();
		for(String varName:allVars.keySet()) {
			if(varName.startsWith(CUSTOM_ATTRIBUTES_PREFIX)) {
				String attributeKey = varName.substring(CUSTOM_ATTRIBUTES_PREFIX.length());
				result.put(attributeKey, (String) allVars.get(varName));
			}
		}
		return result;
	}
}
