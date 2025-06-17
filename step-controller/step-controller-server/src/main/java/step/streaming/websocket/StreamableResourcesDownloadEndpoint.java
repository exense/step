package step.streaming.websocket;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.io.stream.JsonMessage;
import step.grid.io.stream.download.RequestChunkMessage;
import step.grid.io.stream.download.ResourceStatusChangedMessage;
import step.grid.websockets.WebsocketServerEndpointSessionsHandler;
import step.streaming.StreamableResourceChangedListener;
import step.grid.io.stream.StreamableResourceStatus;
import step.streaming.StreamableResourceExtendedStatus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class StreamableResourcesDownloadEndpoint extends Endpoint implements StreamableResourceChangedListener {

    public static final String ENDPOINT_URL = "/ws/resource/download/{id}";
    public static final String ENDPOINT_URL_WITHOUT_ID = "/ws/resource/download/";

    public static ServerEndpointConfig getEndpointConfig(StreamableResourcesEndpointsConfiguration configuration) {
        return ServerEndpointConfig.Builder.create(StreamableResourcesDownloadEndpoint.class, ENDPOINT_URL)
                .configurator(new ServerEndpointConfig.Configurator() {
                    @Override
                    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                        return endpointClass.cast(new StreamableResourcesDownloadEndpoint(configuration));
                    }
                })
                .build();
    }

    private static final Logger logger = LoggerFactory.getLogger(StreamableResourcesDownloadEndpoint.class);

    private final StreamableResourcesEndpointsConfiguration config;
    private Session session;
    private String resourceId;
    private String filename;

    public StreamableResourcesDownloadEndpoint(StreamableResourcesEndpointsConfiguration config) {
        this.config = config;
    }

    @Override
    public String toString() {
        return String.format("StreamableResourcesDownloadEndpoint{session=%s, resourceId=%s, filename=%s}", Optional.ofNullable(session).map(Session::getId), resourceId, filename);
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        this.resourceId = Objects.requireNonNull(getPathParams(ENDPOINT_URL, session.getRequestURI().getPath()).get("id"));
        this.session = session;
        session.addMessageHandler(String.class, this::onMessage);
        WebsocketServerEndpointSessionsHandler.getInstance().register(session);
        logger.info("session opened: {}", this);
        // this will immediately trigger a first "update" event so we can send the current status to the client,
        // or it throws an exception on invalid ID.
        config.manager.registerResourceChangedListener(resourceId, this, filename -> this.filename = filename);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        config.manager.unregisterResourceChangedListener(resourceId, this);
        WebsocketServerEndpointSessionsHandler.getInstance().unregister(session);
        logger.info("session closed: {}, closeReason={}", this, closeReason);
    }

    @Override
    public void onError(Session session, Throwable thr) {
        logger.error("session error: {}", this, thr);
    }

    private void onMessage(String json) {
        // we only expect RequestChunkMessages from the client, anything else will throw an exception
        RequestChunkMessage request = JsonMessage.fromString(json);
        logger.info("{}: received request: {}", this, request.toString());
        try {
            long bytesSent = config.manager.handleDownload(resourceId, session.getBasicRemote().getSendStream(), request.startOffset, request.endOffset);
            logger.info("{}: sent {} bytes", this, bytesSent);
        } catch (IOException e) {
            logger.error("error handling download: {}", this, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onResourceUpdated(String resourceId, StreamableResourceStatus status, Long currentSize) {
        session.getAsyncRemote().sendText(new ResourceStatusChangedMessage(status, currentSize).toString());
    }

    // If this is used in more than one place, it could be extracted to a common superclass.
    protected Map<String, String> getPathParams(String template, String path) {
        String[] tmplParts = template.split("/");
        String[] pathParts = path.split("/");

        if (tmplParts.length != pathParts.length) {
            throw new IllegalArgumentException("Path and template length mismatch");
        }

        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < tmplParts.length; i++) {
            if (tmplParts[i].startsWith("{") && tmplParts[i].endsWith("}")) {
                String key = tmplParts[i].substring(1, tmplParts[i].length() - 1);
                params.put(key, pathParts[i]);
            }
        }
        return params;
    }
}
