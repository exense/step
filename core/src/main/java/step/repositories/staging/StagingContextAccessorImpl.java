package step.repositories.staging;

import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.MongoClientSession;

public class StagingContextAccessorImpl extends AbstractCRUDAccessor<StagingContext> {

	public StagingContextAccessorImpl(MongoClientSession clientSession) {
		super(clientSession, "staging", StagingContext.class);
	}

}
