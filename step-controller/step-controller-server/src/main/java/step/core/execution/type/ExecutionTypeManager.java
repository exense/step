package step.core.execution.type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExecutionTypeManager {

	Map<String, ExecutionType> registry = new ConcurrentHashMap<>();

	public ExecutionType get(String executionTypeName) {
		return registry.get(executionTypeName!=null?executionTypeName:"Default");
	}

	public ExecutionType put(ExecutionType executionType) {
		return registry.put(executionType.getName(), executionType);
	}	
}
