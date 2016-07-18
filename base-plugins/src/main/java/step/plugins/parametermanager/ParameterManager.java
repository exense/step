package step.plugins.parametermanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import step.commons.activation.Activator;

public class ParameterManager {
	
	Map<String, List<Parameter>> parameterMap = new HashMap<String, List<Parameter>>();
	
	public void clearParameters() {
		parameterMap.clear();
	}
	
	public void addParameters(List<Parameter> parameters) throws ScriptException {
		for(Parameter parameter:parameters) {
			addParameter(parameter);
		}
	}
	
	public void addParameter(Parameter parameter) throws ScriptException {
		Activator.compileActivationExpression(parameter);
		
		List<Parameter> parameters = parameterMap.get(parameter.key);
		if(parameters==null) {
			parameters = new ArrayList<>();
			parameterMap.put(parameter.key, parameters);
		}
		
		parameters.add(parameter);
	}
	
	public Map<String, String> getAllParameters(Map<String, Object> contextBindings) {
		Map<String, String> result = new HashMap<>();
		
		Bindings bindings = contextBindings!=null?new SimpleBindings(contextBindings):null;
		
		for(String key:parameterMap.keySet()) {
			List<Parameter> parameters = parameterMap.get(key);
			Parameter bestMatch = Activator.findBestMatch(bindings, parameters);
			if(bestMatch!=null) {
				result.put(key, bestMatch.value);
			}
		}
		return result;
	}
}
