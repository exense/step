package step.controller.multitenancy.accessor;

import step.controller.multitenancy.model.Project;
import step.core.accessors.AbstractAccessor;
import step.core.collections.inmemory.InMemoryCollection;

public class InMemoryProjectAccessor extends AbstractAccessor<Project> implements ProjectAccessor  {

	public InMemoryProjectAccessor() {
		super(new InMemoryCollection<Project>());
	}

}
