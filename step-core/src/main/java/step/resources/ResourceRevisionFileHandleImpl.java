package step.resources;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class ResourceRevisionFileHandleImpl implements Closeable, ResourceRevisionFileHandle {

	private final ResourceManagerImpl resourceManager;
	private final Resource resource;
	private final File resourceFile;
	
	public ResourceRevisionFileHandleImpl(ResourceManagerImpl resourceManager, Resource resource, File resourceFile) {
		super();
		this.resourceManager = resourceManager;
		this.resource = resource;
		this.resourceFile = resourceFile;
	}

	@Override
	public File getResourceFile() {
		return resourceFile;
	}

	@Override
	public void close() throws IOException {
		resourceManager.closeResourceRevisionContent(resource);
	}
}
