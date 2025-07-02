package step.streaming;

import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;

public class StreamingResourceAccessor extends AbstractAccessor<StreamingResource> {
    public StreamingResourceAccessor(Collection<StreamingResource> collectionDriver) {
        super(collectionDriver);
    }
}
