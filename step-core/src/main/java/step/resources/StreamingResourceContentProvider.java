package step.resources;

import java.io.InputStream;

/**
 * This class serves as a simple and targeted abstraction layer to get access to the content of streamed resources.
 */
public interface StreamingResourceContentProvider {
    InputStream getResourceContentStream(String resourceId) throws Exception;
}
