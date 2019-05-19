package step.core.scheduler;

import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.Collectors;

import step.core.accessors.InMemoryCRUDAccessor;

public class InMemoryExecutionTaskAccessor extends InMemoryCRUDAccessor<ExecutiontTaskParameters> implements ExecutionTaskAccessor {

	@Override
	public Iterator<ExecutiontTaskParameters> getActiveExecutionTasks() {
		return map.values().stream().filter(e->e.active).sorted(new Comparator<ExecutiontTaskParameters>() {

			@Override
			public int compare(ExecutiontTaskParameters o1, ExecutiontTaskParameters o2) {
				return o1.getId().compareTo(o2.getId());
			}
		}).collect(Collectors.toList()).iterator();
	}


}
