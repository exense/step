package step.functions;

import java.util.Map;

public interface FunctionRepository {

	public Function getFunctionByAttributes(Map<String, String> attributes);
	
	public Function getFunctionById(String id);
	
	public FunctionConfiguration getFunctionConfigurationById(String functionId);
	
	
}
