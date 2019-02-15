package step.resources;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;


public class ResourceRevisionContentImpl implements Closeable, ResourceRevisionContent {

	private final InputStream resourceStream;
	private final String resourceName;
	private final Resource resource;
	private final ResourceManagerImpl resourceManager;
	
	protected ResourceRevisionContentImpl (ResourceManagerImpl resourceManager, Resource resource, InputStream resourceStream, String resourceName) {
		super();
		this.resource = resource;
		this.resourceStream = resourceStream;
		this.resourceName = resourceName;
		this.resourceManager = resourceManager;
	}

	@Override
	public InputStream getResourceStream() {
		return resourceStream;
	}

	@Override
	public String getResourceName() {
		return resourceName;
	}

	@Override
	public void close() throws IOException {
		resourceStream.close();
		resourceManager.closeResourceRevisionContent(resource);
	}
}
