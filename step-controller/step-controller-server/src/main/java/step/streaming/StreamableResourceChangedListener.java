package step.streaming;

import step.grid.io.stream.StreamableResourceStatus;

public interface StreamableResourceChangedListener {
    void onResourceUpdated(String resourceId, StreamableResourceStatus status, Long currentSize);
}
