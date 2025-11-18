package step.automation.packages;

import step.core.collections.inmemory.InMemoryCollection;
import step.resources.*;

import java.io.File;

public class TestResourceManagerImpl extends LocalResourceManagerImpl {

    public TestResourceManagerImpl() {
        super(new File("resources"), new InMemoryTestResourceAccessor(), new InMemoryTestResourceRevisionAccessor());
    }

    private static class InMemoryTestResourceAccessor extends AbstractResourceAccessor implements ResourceAccessor {

        public InMemoryTestResourceAccessor() {
            super(new InMemoryCollection<Resource>(false));
        }
    }

    private static class InMemoryTestResourceRevisionAccessor extends ResourceRevisionAccessorImpl {

        public InMemoryTestResourceRevisionAccessor() {
            super(new InMemoryCollection<ResourceRevision>(false));
        }
    }
}
