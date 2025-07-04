package step.functions.handler.liveupload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class LiveUploadContext {
    private static final Logger logger = LoggerFactory.getLogger(LiveUploadContext.class);
    public static final String REQUEST_PARAMETER_NAME = "functionContext";
    public static final String PROPERTY_KEY = "$functionHandlerLiveUploadUri";

    private static URI baseUri = null;

    public final String contextId;

    private LiveUploadContext(String contextId) {
        this.contextId = contextId;
    }

    public URI getContextUri() {
        return addQueryParam(baseUri, contextId);
    }

    public static LiveUploadContext createNew() {
        if (baseUri == null) {
            throw new IllegalStateException("Base URI not set, unable to create LiveUploadContext");
        }
        return new LiveUploadContext(UUID.randomUUID().toString());
    }

    public static void setBaseUri(String uri) {
        if (baseUri != null) {
            throw new IllegalStateException("Base URI already set");
        }
        baseUri = URI.create(uri);
        logger.info("Base URI set to: {}", baseUri);
    }

    private static URI addQueryParam(URI originalUri, String value) {
        try {
            return new URI(
                    originalUri.getScheme(), originalUri.getAuthority(), originalUri.getPath(),
                    REQUEST_PARAMETER_NAME + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8),
                    originalUri.getFragment()
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return getContextUri().toString();
    }


}
