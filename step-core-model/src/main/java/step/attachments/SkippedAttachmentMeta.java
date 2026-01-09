package step.attachments;

public class SkippedAttachmentMeta extends AttachmentMeta {

    private String reason;

    public SkippedAttachmentMeta() {
        super(null);
    }

    public SkippedAttachmentMeta(String name, String mimeType, String reason) {
        super(null);
        this.name = name;
        this.mimeType = mimeType;
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
