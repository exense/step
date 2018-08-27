package step.core.scheduler;

import java.util.Iterator;

import step.core.accessors.InMemoryCRUDAccessor;

public class InMemoryExecutionTaskAccessor extends InMemoryCRUDAccessor<ExecutiontTaskParameters> implements ExecutionTaskAccessor {

	@Override
	public Iterator<ExecutiontTaskParameters> getActiveExecutionTasks() {
		throw new RuntimeException("Not implemented");
	}


}
