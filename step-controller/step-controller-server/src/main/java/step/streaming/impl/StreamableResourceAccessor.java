package step.streaming.impl;

import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;

public class StreamableResourceAccessor extends AbstractAccessor<StreamableResource> {
    public StreamableResourceAccessor(Collection<StreamableResource> collectionDriver) {
        super(collectionDriver);
    }
}
