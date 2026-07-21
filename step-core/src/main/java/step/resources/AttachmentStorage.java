package step.resources;

import step.attachments.AttachmentMeta;

import java.io.InputStream;

public interface AttachmentStorage {
    InputStream getAttachmentStream(String attachmentId) throws Exception;

    /**
     * Saves an attachment in the given executionContent, with the given metadata.
     * Implementation note: the executionContext will be a step.core.execution.ExecutionContext, but that class
     * is out of scope here, hence the use of a generic Object.
     *
     * @param executionContext execution context
     * @param content          content of the attachment, as a byte array
     * @param filename         file name of the attachment
     * @param mimeType         MIME type, may be null
     * @return AttachmentMeta object corresponding to the attachment
     */
    AttachmentMeta saveAttachment(Object executionContext, byte[] content, String filename, String mimeType);

    default void cleanupIfNeeded() {
    }
}
