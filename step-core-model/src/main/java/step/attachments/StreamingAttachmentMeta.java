package step.attachments;

import org.bson.types.ObjectId;

public class StreamingAttachmentMeta extends AttachmentMeta {

    // This contains exactly the same values as the step.streaming.common.StreamingResourceTransferStatus enum;
    // duplicated to avoid pulling in the full dependency.
    public enum Status {
        INITIATED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
    }

    private String mimeType;
    private Long currentSize;
    private Status status;

    public StreamingAttachmentMeta() {
        super(null);
    }

    public StreamingAttachmentMeta(ObjectId id, String name, String mimeType) {
        super(id);
        this.name = name;
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getCurrentSize() {
        return currentSize;
    }

    public void setCurrentSize(Long currentSize) {
        this.currentSize = currentSize;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "StreamingAttachmentMeta{" +
                "id='" + _id.toHexString() + '\'' +
                ", name='" + name + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", currentSize=" + currentSize +
                ", status=" + status +
                '}';
    }
}
