package step.streaming;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.io.stream.StreamableResourceStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

// This is a class coordinating everything about streamable resources. It's written at an abstraction layer
// That does not necessarily depend on the step-framework classes (there's the actual impl/ package which does).
// I'm not 100% happy with the method naming/logic etc., but it does the job for now.

public class StreamableResourcesManager {
    private static final Logger logger = LoggerFactory.getLogger(StreamableResourcesManager.class);
    private final StreamableResourcesStorageBackend storage;
    private final StreamableResourcesCatalogBackend catalog;
    private final Map<String, List<StreamableResourceChangedListener>> changeListeners = new ConcurrentHashMap<>();

    public StreamableResourcesManager(Function<StreamableResourcesManager, StreamableResourcesCatalogBackend> catalogConstructor,
                                      Function<StreamableResourcesManager, StreamableResourcesStorageBackend> storageConstructor) {
        // this is a pretty neat way of constructing mutually dependent objects where both can have a final reference to each other :-)
        this.catalog = catalogConstructor.apply(this);
        this.storage = storageConstructor.apply(this);
    }

    // invoked by upload endpoint
    public String handleNewUploadRequest(String filename, String contentMimeType) {
        String id = catalog.createId(filename, contentMimeType);
        storage.prepareForNewId(id);
        return id;
    }

    // invoked by upload endpoint
    public long handleUploadData(String resourceId, InputStream stream) throws IOException {
        return storage.handleUpload(resourceId, stream);
    }

    // invoked by upload endpoint (FINISHED/FAILURE), or storage backend (IN_PROGRESS). size and status may be null if no change required.
    public void onUploadStatusChanged(String resourceId, Long newSize, StreamableResourceStatus status) {
        catalog.updateStatus(resourceId, newSize, status);
    }

    // invoked by download endpoint to request chunks of data
    public long handleDownload(String resourceId, OutputStream outputStream, long startPosition, long endPosition) throws IOException {
        return storage.handleDownload(resourceId, outputStream, startPosition, endPosition);
    }

    // invoked by catalog in response to creation or status change
    public void onResourceUpdated(String resourceId, StreamableResourceExtendedStatus status) {
        logger.info("Streamable resource was updated: {} size now {}, status {}", resourceId, status.currentSize, status.status);
        List<StreamableResourceChangedListener> listeners = changeListeners.get(resourceId);
        if (listeners != null) {
            for (StreamableResourceChangedListener listener : listeners) {
                listener.onResourceUpdated(resourceId, status.status, status.currentSize);
            }
        }
        if (status.status == StreamableResourceStatus.FAILED) {
            storage.handleFailedUpload(resourceId);
        }
    }

    // This method is called by the download clients, to get updates while resources are uploaded
    // We make the IllegalArgumentException explicit here (though not strictly needed as it's a RuntimeException)
    // This will also immediately send a first status update with the current status.
    public void registerResourceChangedListener(String resourceId, StreamableResourceChangedListener listener, Consumer<String> filenameConsumer) throws IllegalArgumentException {
        // throws exception on invalid id
        StreamableResourceExtendedStatus status = catalog.getCurrentStatus(resourceId);
        if (filenameConsumer != null) {
            filenameConsumer.accept(status.filename);
        }
        changeListeners.computeIfAbsent(resourceId, k -> new CopyOnWriteArrayList<>()).add(listener);
        listener.onResourceUpdated(resourceId, status.status, status.currentSize);
    }

    public void unregisterResourceChangedListener(String resourceId, StreamableResourceChangedListener listener) {
        List<StreamableResourceChangedListener> listeners = changeListeners.get(resourceId);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                changeListeners.remove(resourceId);
            }
        }
    }
}
