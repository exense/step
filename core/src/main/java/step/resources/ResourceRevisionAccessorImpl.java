package step.resources;

import java.util.Iterator;

import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.MongoClientSession;

public class ResourceRevisionAccessorImpl extends AbstractCRUDAccessor<ResourceRevision> implements ResourceRevisionAccessor {

	public ResourceRevisionAccessorImpl(MongoClientSession clientSession) {
		super(clientSession, "resourceRevisions", ResourceRevision.class);
	}

	@Override
	public Iterator<ResourceRevision> getResourceRevisionsByResourceId(String resourceId) {
		return collection.find("{resourceId: #}", resourceId).as(ResourceRevision.class).iterator();
	}

	@Override
	public Iterator<ResourceRevision> getResourceRevisionsByChecksum(String checksum) {
		return collection.find("{checksum: #}", checksum).as(ResourceRevision.class).iterator();
	}

}
