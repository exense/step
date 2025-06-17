package step.streaming.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.io.stream.data.CheckpointingOutputStream;
import step.grid.io.stream.StreamableResourceStatus;
import step.grid.io.stream.data.StreamableFiles;
import step.streaming.StreamableResourcesManager;
import step.streaming.StreamableResourcesStorageBackend;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;

public class StreamableResourcesFilesystemStorageBackend implements StreamableResourcesStorageBackend {
    private static final Logger logger = LoggerFactory.getLogger(StreamableResourcesFilesystemStorageBackend.class);
    private final StreamableResourcesManager manager;
    private final File baseDirectory;
    private final boolean hashIdsBeforeStoring;

    public StreamableResourcesFilesystemStorageBackend(StreamableResourcesManager streamableResourcesManager, File baseDirectory, boolean hashIdsBeforeStoring) {
        this.manager = streamableResourcesManager;
        this.baseDirectory = validateBaseDirectory(baseDirectory);
        this.hashIdsBeforeStoring = hashIdsBeforeStoring;
    }

    private File validateBaseDirectory(File baseDirectory) {
        if (baseDirectory.exists() && !baseDirectory.isDirectory()) {
            throw new IllegalArgumentException(String.format("%s is not a directory", baseDirectory.getAbsolutePath()));
        }
        if (!baseDirectory.exists() && !baseDirectory.mkdirs()) {
            throw new RuntimeException(String.format("%s could not be created", baseDirectory.getAbsolutePath()));
        }
        return baseDirectory;
    }

    @Override
    public void prepareForNewId(String id) {
        File f= getFileForId(id, true);
        logger.info("Preparing for new id {}, using file {}", id, f);
    }

    private File getFileForId(String id, boolean createDirectories) {
        String path = hashIdsBeforeStoring ? hashId(id) : id;
        File resolvedFile = resolveFile(path, id);
        if (createDirectories) {
            if (!resolvedFile.getParentFile().mkdirs()) {
                throw new RuntimeException(String.format("%s could not be created", resolvedFile.getParentFile().getAbsolutePath()));
            }
        }
        return resolvedFile;
    }

    public static String hashId(String id) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString(); // e.g. "5d41402abc4b2a76b9719d911017c592"
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    private File resolveFile(String path, String id) {
        // This will create "partial prefix" subdirectories to distribute the data better. If IDs tend to start with the same prefixes (as ObjectIds do),
        // consider hashing them for better distribution.
        String relativePath = File.separator + path.substring(0, 2) + File.separator + path.substring(2, 4) + File.separator + id + ".bin";
        return new File(baseDirectory, relativePath);
    }

    @Override
    public long handleUpload(String resourceId, InputStream inputStream) throws IOException {
        File file = getFileForId(resourceId, false);
        Consumer<Long> flushListener = size -> manager.onUploadStatusChanged(resourceId, size, StreamableResourceStatus.UPLOADING);
        try (inputStream; OutputStream out = new CheckpointingOutputStream(getOutputStream(file, file.length()), CheckpointingOutputStream.DEFAULT_FLUSH_INTERVAL_MILLIS, flushListener)) {
            inputStream.transferTo(out);
        }
        return file.length();
    }

    public long handleDownload(String resourceId, OutputStream outputStream, long startPosition, long endPosition) throws IOException {
        File file = getFileForId(resourceId, false);
        try (outputStream; InputStream in = getInputStream(file, startPosition, endPosition)) {
            return in.transferTo(outputStream);
        }
    }

    @Override
    public void handleFailedUpload(String resourceId) {
        File file = getFileForId(resourceId, false);
        if (file.exists()) {
            boolean deleted = file.delete();
            logger.info("Deleting file {} after failed upload; deleted={}", file, deleted);
        }
    }

    private OutputStream getOutputStream(File file, long startPosition) throws IOException {
        return StreamableFiles.getOutputStream(file, startPosition);
    }

    private InputStream getInputStream(File file, long startPosition, long endPosition) throws IOException {
        return StreamableFiles.getInputStream(file, startPosition, endPosition);
    }

}
