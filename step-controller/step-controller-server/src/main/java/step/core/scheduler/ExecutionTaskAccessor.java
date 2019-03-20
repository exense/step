package step.core.scheduler;

import java.util.Iterator;

import step.core.accessors.CRUDAccessor;

public interface ExecutionTaskAccessor extends CRUDAccessor<ExecutiontTaskParameters> {

	Iterator<ExecutiontTaskParameters> getActiveExecutionTasks();

}