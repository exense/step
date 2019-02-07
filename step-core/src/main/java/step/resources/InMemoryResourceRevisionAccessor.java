package step.resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import step.core.accessors.InMemoryCRUDAccessor;

public class InMemoryResourceRevisionAccessor extends InMemoryCRUDAccessor<ResourceRevision>
		implements ResourceRevisionAccessor {

	@Override
	public Iterator<ResourceRevision> getResourceRevisionsByResourceId(String resourceId) {
		List<ResourceRevision> result = new ArrayList<>();
		getAll().forEachRemaining(r->{
			if(r.getResourceId().equals(resourceId)) {
				result.add(r);
			}
		});;
		return result.iterator();
	}

	@Override
	public Iterator<ResourceRevision> getResourceRevisionsByChecksum(String checksum) {
		List<ResourceRevision> result = new ArrayList<>();
		getAll().forEachRemaining(r->{
			if(r.getChecksum().equals(checksum)) {
				result.add(r);
			}
		});;
		return result.iterator();
	}

}
