package step.streaming;

import org.bson.types.ObjectId;
import step.core.GlobalContext;
import step.streaming.common.StreamingResourceMetadata;
import step.streaming.common.StreamingResourceStatus;
import step.streaming.common.StreamingResourceTransferStatus;
import step.streaming.server.StreamingResourcesCatalogBackend;

public class StreamingResourceCollectionCatalogBackend implements StreamingResourcesCatalogBackend {
    private final StreamingResourceAccessor accessor;
    public StreamingResourceCollectionCatalogBackend(GlobalContext context) {
        accessor = new StreamingResourceAccessor(context.getCollectionFactory().getCollection(StreamingResource.COLLECTION_NAME, StreamingResource.class));
    }
    @Override
    public String createResource(StreamingResourceMetadata metadata) {
        StreamingResource entity = new StreamingResource();
        entity.filename = metadata.getFilename();
        entity.mimeType = metadata.getMimeType();
        entity.status = new StreamingResourceStatus(StreamingResourceTransferStatus.INITIATED, 0L);
        return accessor.save(entity).getId().toHexString();
    }

    @Override
    public void updateStatus(String resourceId, StreamingResourceStatus status) {
        StreamingResource entity = getEntity(resourceId);
        if (!entity.status.equals(status)) {
            entity.status = status;
            accessor.save(entity);
        }
    }

    private StreamingResource getEntity(String resourceId) {
        StreamingResource entity = accessor.get(new ObjectId(resourceId));
        if (entity == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceId);
        }
        return entity;
    }

    @Override
    public StreamingResourceStatus getStatus(String resourceId) {
        return getEntity(resourceId).status;
    }
}
