package step.streaming;

import step.grid.io.stream.StreamableResourceStatus;

public class StreamableResourceExtendedStatus {
    String filename;
    StreamableResourceStatus status;
    Long currentSize;

    public StreamableResourceExtendedStatus(String filename, StreamableResourceStatus status, Long currentSize) {
        this.filename = filename;
        this.status = status;
        this.currentSize = currentSize;
    }
}

