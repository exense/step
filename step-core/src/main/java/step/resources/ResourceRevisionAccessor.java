package step.resources;

import java.util.Iterator;

import step.core.accessors.CRUDAccessor;

public interface ResourceRevisionAccessor extends CRUDAccessor<ResourceRevision> {
	
	public Iterator<ResourceRevision> getResourceRevisionsByResourceId(String resourceId);

	public Iterator<ResourceRevision> getResourceRevisionsByChecksum(String checksum);
}
