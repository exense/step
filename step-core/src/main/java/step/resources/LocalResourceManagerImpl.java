package step.resources;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class LocalResourceManagerImpl extends ResourceManagerImpl {

	public LocalResourceManagerImpl() {
		super(new File("resources"), new InMemoryResourceAccessor(), new InMemoryResourceRevisionAccessor());
	}
	
	public LocalResourceManagerImpl(File folder) {
		super(folder, new InMemoryResourceAccessor(), new InMemoryResourceRevisionAccessor());
	}
	
	public void cleanup() {
		if (resourceRootFolder.exists() && resourceRootFolder.isDirectory() && resourceRootFolder.canWrite()) {
			try {
				FileUtils.deleteDirectory(resourceRootFolder);
			} catch (IOException e) {
				logger.error("Could not remove local resource folder: " + resourceRootFolder.getAbsolutePath(),e );
			}
		}
	}

}
