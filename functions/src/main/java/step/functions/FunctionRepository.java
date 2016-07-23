package step.functions;

public interface FunctionRepository {

	public Function getFunctionById(String id);
	
	public FunctionConfiguration getFunctionConfigurationById(String functionId);
	
	
}
