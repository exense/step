package step.plugins.streaming;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.constants.StreamingConstants;
import step.core.GlobalContext;
import step.core.access.User;
import step.core.objectenricher.ObjectEnricher;
import step.framework.server.Session;
import step.streaming.common.StreamingResourceMetadata;
import step.streaming.common.StreamingResourceStatus;
import step.streaming.common.StreamingResourceTransferStatus;
import step.streaming.common.StreamingResourceUploadContext;
import step.streaming.server.StreamingResourceStatusUpdate;
import step.streaming.server.StreamingResourcesCatalogBackend;

import java.util.Optional;

public class StreamingResourceCollectionCatalogBackend implements StreamingResourcesCatalogBackend {
    private static final Logger logger = LoggerFactory.getLogger(StreamingResourceCollectionCatalogBackend.class);
    private final StreamingResourceAccessor accessor;
    public StreamingResourceCollectionCatalogBackend(GlobalContext context) {
        accessor = new StreamingResourceAccessor(context.getCollectionFactory().getCollection(StreamingResource.COLLECTION_NAME, StreamingResource.class));
    }
    @Override
    public String createResource(StreamingResourceMetadata metadata, StreamingResourceUploadContext context) {
        StreamingResource entity = new StreamingResource();
        entity.filename = metadata.getFilename();
        entity.mimeType = metadata.getMimeType();
        entity.status = new StreamingResourceStatus(StreamingResourceTransferStatus.INITIATED, 0L, metadata.getSupportsLineAccess() ? 0L : null);
        // FIXME: Currently an upload context is not strictly *required* (nor is the enricher) - do we want to enforce it?
        Optional<ObjectEnricher> maybeObjectEnricher = Optional.ofNullable(context)
                .map(c -> (ObjectEnricher) c.getAttributes().get(StreamingConstants.AttributeNames.ACCESS_CONTROL_ENRICHER));
        maybeObjectEnricher.ifPresent(e -> e.accept(entity));
        return accessor.save(entity).getId().toHexString();
    }

    @Override
    public StreamingResourceStatus updateStatus(String resourceId, StreamingResourceStatusUpdate statusUpdate) {
        StreamingResource entity = getEntity(resourceId);
        StreamingResourceStatus newStatus = statusUpdate.applyTo(entity.status);
        if (!entity.status.equals(newStatus)) {
            entity.status = newStatus;
            accessor.save(entity);
            logger.debug("Updating status of resource {}: {}", resourceId, newStatus);
        }
        return newStatus;
    }

    StreamingResource getEntity(String resourceId) {
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
