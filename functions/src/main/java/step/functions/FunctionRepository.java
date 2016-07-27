package step.functions;

import java.util.Map;

public interface FunctionRepository {

	public Function getFunctionByAttributes(Map<String, String> attributes);
	
	public Function getFunctionById(String id);
	
	public void addFunction(Function function);
	
	public void deleteFunction(String functionId);
	
	
}
