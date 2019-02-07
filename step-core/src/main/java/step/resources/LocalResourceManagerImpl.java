package step.resources;

import java.io.File;

public class LocalResourceManagerImpl extends ResourceManagerImpl {

	public LocalResourceManagerImpl() {
		super(new File("resources"), new InMemoryResourceAccessor(), new InMemoryResourceRevisionAccessor());
	}

}
