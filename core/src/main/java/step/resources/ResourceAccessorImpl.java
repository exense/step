package step.resources;

import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.MongoClientSession;

public class ResourceAccessorImpl extends AbstractCRUDAccessor<Resource> implements ResourceAccessor {

	public ResourceAccessorImpl(MongoClientSession clientSession) {
		super(clientSession, "resources", Resource.class);
	}

}
