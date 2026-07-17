package step.resources;

import step.attachments.AttachmentMeta;

import java.io.InputStream;

public interface AttachmentStorage {
    InputStream getAttachmentStream(String attachmentId) throws Exception;

    AttachmentMeta saveAttachment(Object executionContext, byte[] content, String filename, String mimeType);

    default void cleanupIfNeeded() {
    }
}
