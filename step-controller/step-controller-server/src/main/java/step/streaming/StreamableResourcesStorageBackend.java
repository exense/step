package step.streaming;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface StreamableResourcesStorageBackend {
    void prepareForNewId(String resourceId);

    long handleUpload(String resourceId, InputStream stream) throws IOException;

    void handleFailedUpload(String resourceId);

    long handleDownload(String resourceId, OutputStream outputStream, long startPosition, long endPosition) throws IOException;
}
