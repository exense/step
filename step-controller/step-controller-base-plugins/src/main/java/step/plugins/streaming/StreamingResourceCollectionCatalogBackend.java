package step.plugins.streaming;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.constants.LiveReportingConstants;
import step.core.GlobalContext;
import step.core.objectenricher.ObjectEnricher;
import step.streaming.common.StreamingResourceMetadata;
import step.streaming.common.StreamingResourceStatus;
import step.streaming.common.StreamingResourceTransferStatus;
import step.streaming.common.StreamingResourceUploadContext;
import step.streaming.server.StreamingResourceStatusUpdate;
import step.streaming.server.StreamingResourcesCatalogBackend;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class StreamingResourceCollectionCatalogBackend implements StreamingResourcesCatalogBackend {
    private static final Logger logger = LoggerFactory.getLogger(StreamingResourceCollectionCatalogBackend.class);
    final StreamingResourceAccessor accessor;

    public StreamingResourceCollectionCatalogBackend(GlobalContext context) {
        this(new StreamingResourceAccessor(context.getCollectionFactory().getCollection(StreamingResource.COLLECTION_NAME, StreamingResource.class)));
    }

    public StreamingResourceCollectionCatalogBackend(StreamingResourceAccessor accessor) {
        this.accessor = accessor;
    }

    @Override
    public String createResource(StreamingResourceMetadata metadata, StreamingResourceUploadContext context) {
        StreamingResource entity = new StreamingResource();
        entity.filename = metadata.getFilename();
        entity.mimeType = metadata.getMimeType();
        entity.status = new StreamingResourceStatus(StreamingResourceTransferStatus.INITIATED, 0L, metadata.getSupportsLineAccess() ? 0L : null);

        // FIXME: Currently an upload context is not strictly *required* (nor is the enricher) - do we want to enforce it?
        Optional<StreamingResourceUploadContext> maybeContext = Optional.ofNullable(context);
        maybeContext.map(c -> (ObjectEnricher) c.getAttributes().get(LiveReportingConstants.ACCESSCONTROL_ENRICHER))
                .ifPresent(enricher -> enricher.accept(entity));
        maybeContext.map(c -> (String) c.getAttributes().get(LiveReportingConstants.CONTEXT_EXECUTION_ID))
                .ifPresent(id -> entity.addAttribute(StreamingResource.ATTRIBUTE_EXECUTION_ID, id));

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

    public Stream<String> findResourceIdsForExecution(String executionId) {
        return accessor.findManyByCriteria(Map.of("attributes." + StreamingResource.ATTRIBUTE_EXECUTION_ID, executionId))
                .map(r -> r.getId().toHexString());
    }

    @Override
    public void delete(String resourceId) {
        accessor.remove(new ObjectId(resourceId));
    }
}
