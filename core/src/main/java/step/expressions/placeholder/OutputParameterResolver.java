package step.expressions.placeholder;

import java.util.Map;

public class OutputParameterResolver implements Resolver {

	final Map<String, String> outputParameterMap;
	
	public OutputParameterResolver(Map<String, String> outputParameterMap) {
		super();
		this.outputParameterMap = outputParameterMap;
	}

	@Override
	public String resolve(String name) {
		String result = outputParameterMap.get(name.trim()); 
		return result;
	}

}
