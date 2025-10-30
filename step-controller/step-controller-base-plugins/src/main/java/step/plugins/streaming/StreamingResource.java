package step.plugins.streaming;

import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.EnricheableObject;
import step.streaming.common.StreamingResourceStatus;

public class StreamingResource extends AbstractOrganizableObject implements EnricheableObject {
    public static final String COLLECTION_NAME = "streaming_resources";
    public static final String ATTRIBUTE_EXECUTION_ID = "executionId";
    public String filename;
    public String mimeType;
    public StreamingResourceStatus status;
}
