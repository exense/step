package step.resources;

import org.apache.commons.io.FileUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.AttachmentMeta;
import step.attachments.SkippedAttachmentMeta;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Local (i.e. "non-Step-infrastructure") implementation of Attachment Storage,
 * which is used for instance when running Unit Tests, Step JUnit runner, or local
 * executions using the Step CLI.
 * <p>
 * It's a really simple implementation that stores files in the given root directory.
 * In theory, we could use only the IDs, but we append the original filename to remain
 * at least somewhat user-friendly. Another option would be to create a subdirectory
 * per ID, then put a single file in each directory.
 */
public class LocalAttachmentStorage implements AttachmentStorage {
    private static final Logger logger = LoggerFactory.getLogger(LocalAttachmentStorage.class);
    private final File rootDirectory;

    public LocalAttachmentStorage(File rootDirectory) {
        this.rootDirectory = Objects.requireNonNull(rootDirectory);
        if (rootDirectory.exists()) {
            throw new IllegalArgumentException("Attachments storage root directory already exists: " + rootDirectory.getAbsolutePath());
        }
        if (!rootDirectory.mkdirs()) {
            throw new RuntimeException("Cannot create attachments storage directory: " + rootDirectory.getAbsolutePath());
        }
        logger.info("Attachment storage initialized, directory={}", rootDirectory.getAbsolutePath());
    }

    @Override
    public InputStream getAttachmentStream(String attachmentId) throws Exception {
        Path rootPath = rootDirectory.toPath();
        String searchPattern = attachmentId + "_*";

        // Open a directory stream filtering only for files that start with our ID
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath, searchPattern)) {
            for (Path entry : stream) {
                // Since IDs are guaranteed unique, we can safely return an InputStream
                // for the very first match we find.
                return Files.newInputStream(entry);
            }
        }

        // If the loop finishes without returning, the file doesn't exist
        throw new FileNotFoundException("Could not find attachment with ID: " + attachmentId);
    }

    @Override
    public AttachmentMeta saveAttachment(Object executionContext, byte[] content, String filename, String mimeType) {
        // executionContext is not required nor used in this implementation.
        ObjectId id = new ObjectId();
        // This stores to ${ID}_${FILENAME}.
        // We could also use an intermediate directory, ie. ${ID}/${FILENAME}, which will
        // somewhat facilitate lookup but requires twice the number of inodes.
        String storedFilename = id.toHexString() + "_" + filename;
        Path targetPath = rootDirectory.toPath().resolve(storedFilename);
        try {
            // Resolve the path and write the byte array directly
            Files.write(targetPath, content);
            logger.debug("Wrote {} bytes to {}", content.length, targetPath);
            AttachmentMeta meta = new AttachmentMeta(id);
            meta.setName(filename);
            meta.setMimeType(mimeType);
            return meta;
        } catch (IOException e) {
            logger.error("Failed to save attachment {} to {}", targetPath, e);
            return new SkippedAttachmentMeta(filename, mimeType, e.getMessage());
        }
    }

    public void cleanupIfNeeded() {
        try {
            logger.info("Cleanup attachment storage: {}", rootDirectory.getAbsolutePath());
            FileUtils.deleteDirectory(rootDirectory);
        } catch (Exception e) {
            logger.error("Error while cleaning up directory {}", rootDirectory.getAbsolutePath(), e);
        }
    }
}
