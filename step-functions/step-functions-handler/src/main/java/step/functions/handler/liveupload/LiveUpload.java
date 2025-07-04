package step.functions.handler.liveupload;

import step.streaming.common.StreamingResourceMetadata;
import step.streaming.common.StreamingResourceReference;
import step.streaming.common.StreamingResourceStatus;

public class LiveUpload {
    public final StreamingResourceMetadata metadata;
    public final StreamingResourceReference reference;
    public final StreamingResourceStatus status;

    public LiveUpload(StreamingResourceMetadata metadata, StreamingResourceReference reference, StreamingResourceStatus status) {
        this.metadata = metadata;
        this.reference = reference;
        this.status = status;
    }

    @Override
    public String toString() {
        return "LiveUpload{" +
                "status=" + status +
                ", metadata=" + metadata +
                ", reference=" + reference +
                '}';
    }
}
