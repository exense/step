package step.streaming.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.agent.JsonMessageCodec;
import step.grid.io.stream.JsonMessage;
import step.grid.io.stream.StreamableResourceDescriptor;
import step.grid.io.stream.StreamableResourceReference;
import step.grid.io.stream.upload.ReadyForUploadMessage;
import step.grid.io.stream.upload.RequestUploadMessage;
import step.grid.io.stream.upload.UploadProtocolMessage;
import step.grid.websockets.WebsocketServerEndpointSessionsHandler;
import step.grid.io.stream.StreamableResourceStatus;

import java.io.InputStream;
import java.util.Optional;

public class StreamableResourcesUploadEndpoint extends Endpoint {

    public static final String ENDPOINT_URL = "/ws/resource/upload";

    public static ServerEndpointConfig getEndpointConfig(StreamableResourcesEndpointsConfiguration configuration) {
        return ServerEndpointConfig.Builder.create(StreamableResourcesUploadEndpoint.class, ENDPOINT_URL)
                .configurator(new ServerEndpointConfig.Configurator() {
                    @Override
                    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                        return endpointClass.cast(new StreamableResourcesUploadEndpoint(configuration));
                    }
                })
                .build();
    }

    private static final Logger logger = LoggerFactory.getLogger(StreamableResourcesUploadEndpoint.class);

    private final StreamableResourcesEndpointsConfiguration config;
    private Session session;
    // these two are only used to provide more informative logging
    private String resourceId;
    private String filename;

    public StreamableResourcesUploadEndpoint(StreamableResourcesEndpointsConfiguration config) {
        this.config = config;
    }

    @Override
    public String toString() {
        return String.format("StreamableResourcesUploadEndpoint{session=%s, resourceId=%s, filename=%s}", Optional.ofNullable(session).map(Session::getId), resourceId, filename);
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        WebsocketServerEndpointSessionsHandler.getInstance().register(session);
        session.addMessageHandler(String.class, this::onMessage);
        session.addMessageHandler(InputStream.class, this::onStreamData);
        logger.info("session opened: {}", this);
    }

    // This is invoked on error, on server shutdown, and on client disconnect.
    @Override
    public void onClose(Session session, CloseReason closeReason) {
        // TODO: what do we do if an upload is in progress?
        WebsocketServerEndpointSessionsHandler.getInstance().unregister(session);
        logger.info("session closed: {}, closeReason={}", this, closeReason);
    }

    @Override
    public void onError(Session session, Throwable thr) {
        logger.error("session error: {}", this, thr);
    }

    private void onMessage(String json) {
        UploadProtocolMessage message = JsonMessage.fromString(json);
        if (message instanceof RequestUploadMessage) {
            RequestUploadMessage request = (RequestUploadMessage) message;
            filename = request.filename;
            resourceId = config.manager.handleNewUploadRequest(request.filename, request.contentMimeType);
            ReadyForUploadMessage response = new ReadyForUploadMessage();
            StreamableResourceReference ref = new StreamableResourceReference(resourceId, config.uploadEndpoint, config.getDownloadEndpoint(resourceId));
            response.attachmentDescriptor = new StreamableResourceDescriptor(request.filename, request.contentMimeType, ref);
            String responseString = response.toString();
            session.getAsyncRemote().sendText(responseString);
            return;
        }
        throw new IllegalArgumentException("unhandled message: " + message);
    }

    private void onStreamData(InputStream stream) {
        try {
            long newSize =  config.manager.handleUploadData(resourceId, stream);
            config.manager.onUploadStatusChanged(resourceId, newSize, StreamableResourceStatus.FINISHED);
        } catch (Exception e) {
            config.manager.onUploadStatusChanged(resourceId, null, StreamableResourceStatus.FAILED);
            logger.error("error while handling data upload", e);
            throw new RuntimeException(e);
        } finally {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Upload finished"));
            } catch (Exception e) {
                logger.error("error while closing session", e);
            }
        }
    }
}
