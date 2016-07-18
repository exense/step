package step.core.execution;

import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;

import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;

public class InMemoryExecutionAccessor extends ExecutionAccessor {

	Map<String, Execution> map = new HashMap<>();

	@Override
	public void save(Execution execution) {
		if(execution.getId()==null) {
			execution.setId((new ObjectId()).toString());
		}
		map.put(execution.getId(), execution);
	}

	@Override
	public Execution get(String executionId) {
		return map.get(executionId);
	}

}
