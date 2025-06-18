package step.streaming;

import step.grid.io.stream.StreamableResourceStatus;

public interface StreamableResourcesCatalogBackend {
    String createId(String filename, String contentMimeType);

    void updateStatus(String resourceId, Long newSize, StreamableResourceStatus newStatus);

    StreamableResourceExtendedStatus getCurrentStatus(String resourceId) throws IllegalArgumentException;
}
