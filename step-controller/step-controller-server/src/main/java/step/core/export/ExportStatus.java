package step.core.export;

import java.util.Set;

public class ExportStatus {

    private String id;
    private String attachmentID;
    private volatile boolean ready = false;
    private volatile float progress = 0;
    private Set<String> warnings;

    public ExportStatus() {
        super();
    }

    public ExportStatus(String id) {
        super();
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAttachmentID() {
        return attachmentID;
    }

    public void setAttachmentID(String attachmentID) {
        this.attachmentID = attachmentID;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public Set<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(Set<String> warnings) {
        this.warnings = warnings;
    }
}
