package step.core.docker;

import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;

public class DockerRegistryConfigurationAccessor extends AbstractAccessor<DockerRegistryConfiguration> {
    public DockerRegistryConfigurationAccessor(Collection<DockerRegistryConfiguration> collectionDriver) {
        super(collectionDriver);
    }
}
