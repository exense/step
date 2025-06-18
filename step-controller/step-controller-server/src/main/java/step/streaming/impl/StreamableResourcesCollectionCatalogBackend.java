package step.streaming.impl;

import step.core.GlobalContext;
import step.streaming.StreamableResourceExtendedStatus;
import step.streaming.StreamableResourcesCatalogBackend;
import step.streaming.StreamableResourcesManager;
import step.grid.io.stream.StreamableResourceStatus;

import java.util.Objects;

public class StreamableResourcesCollectionCatalogBackend implements StreamableResourcesCatalogBackend {
    private final StreamableResourcesManager manager;
    private final StreamableResourceAccessor accessor;

    public StreamableResourcesCollectionCatalogBackend(StreamableResourcesManager manager, GlobalContext context) {
        this.manager = manager;
        this.accessor = new StreamableResourceAccessor(context.getCollectionFactory().getCollection(StreamableResource.COLLECTION_NAME, StreamableResource.class));
    }

    @Override
    public String createId(String filename, String contentMimeType) {
        StreamableResource resource = new StreamableResource();
        resource.filename = filename;
        resource.contentType = contentMimeType;
        resource.status = StreamableResourceStatus.CREATED;
        resource.currentSize = 0;
        resource = accessor.save(resource);
        String streamableResourceId = resource.getId().toHexString();
        manager.onResourceUpdated(streamableResourceId, makeStatus(resource));
        return streamableResourceId;
    }

    @Override
    public void updateStatus(String resourceId, Long newSize, StreamableResourceStatus newStatus) {
        if (newSize != null || newStatus != null) {
            StreamableResource resource = Objects.requireNonNull(accessor.get(resourceId));
            resource.currentSize = newSize != null ? newSize : resource.currentSize;
            resource.status = newStatus != null ? newStatus : resource.status;
            accessor.save(resource);
            manager.onResourceUpdated(resourceId, makeStatus(resource));
        }
    }

    @Override
    public StreamableResourceExtendedStatus getCurrentStatus(String resourceId) throws IllegalArgumentException {
        StreamableResource resource = accessor.get(resourceId);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceId);
        }
        return makeStatus(resource);
    }

    private StreamableResourceExtendedStatus makeStatus(StreamableResource resource) {
        return new StreamableResourceExtendedStatus(resource.filename, resource.status, resource.currentSize);
    }
}
