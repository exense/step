package step.resources;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;


public class ResourceRevisionContent implements Closeable {

	private final InputStream resourceStream;
	private final String resourceName;
	
	public ResourceRevisionContent (InputStream resourceStream, String resourceName) {
		super();
		this.resourceStream = resourceStream;
		this.resourceName = resourceName;
	}

	public InputStream getResourceStream() {
		return resourceStream;
	}

	public String getResourceName() {
		return resourceName;
	}

	@Override
	public void close() throws IOException {
		resourceStream.close();
	}
}
