package step.streaming.impl;

import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.EnricheableObject;
import step.grid.io.stream.StreamableResourceStatus;

// For now, these sit in their own collection. Could be integrated with the "normal" resources, I guess.
public class StreamableResource extends AbstractOrganizableObject implements EnricheableObject {
    public static final String ENTITY_NAME = "streamableResource";
    public static final String COLLECTION_NAME = "streamableResources";

    public String filename;
    public String contentType;
    public StreamableResourceStatus status;
    public long currentSize;
}
