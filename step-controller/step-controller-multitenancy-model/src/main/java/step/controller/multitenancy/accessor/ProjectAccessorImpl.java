package step.controller.multitenancy.accessor;

import step.controller.multitenancy.model.Project;
import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;


public class ProjectAccessorImpl extends AbstractAccessor<Project> implements ProjectAccessor {

	public ProjectAccessorImpl(Collection<Project> collectionDriver) {
		super(collectionDriver);
	}
}
