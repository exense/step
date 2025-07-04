package step.plugins.streaming;

import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import step.functions.handler.liveupload.LiveUploadContext;
import step.functions.handler.liveupload.LiveUpload;
import step.functions.handler.liveupload.LiveUploadContextListeners;
import step.streaming.common.StreamingResourceMetadata;
import step.streaming.common.StreamingResourceReference;
import step.streaming.common.StreamingResourceStatus;
import step.streaming.server.StreamingResourceManager;
import step.streaming.websocket.server.WebsocketServerEndpointSessionsHandler;
import step.streaming.websocket.server.WebsocketUploadEndpoint;

import java.util.List;
import java.util.function.Consumer;

public class ContextAwareWebsocketUploadEndpoint extends WebsocketUploadEndpoint {

    public ContextAwareWebsocketUploadEndpoint(StreamingResourceManager manager, WebsocketServerEndpointSessionsHandler sessionsHandler) {
        super(manager, sessionsHandler);
    }

    private String liveUploadContextId = null;
    private StreamingResourceMetadata metadata;
    private StreamingResourceReference reference;
    private Consumer<StreamingResourceStatus> statusChangedCallback;

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        session.getRequestParameterMap()
                .getOrDefault(LiveUploadContext.REQUEST_PARAMETER_NAME, List.of())
                .stream().findFirst().ifPresent(ctx -> liveUploadContextId = ctx);
        super.onOpen(session, config);
    }

    @Override
    protected void onUploadReady(StreamingResourceMetadata metadata, StreamingResourceReference reference) {
        if (liveUploadContextId != null) {
            this.metadata = metadata;
            this.reference = reference;
            statusChangedCallback = this::onStatusChanged;
            manager.registerStatusListener(resourceId, statusChangedCallback);
        }
    }

    private void onStatusChanged(StreamingResourceStatus status) {
        LiveUploadContextListeners.notifyListeners(liveUploadContextId, new LiveUpload(metadata, reference, status));
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);
        if (statusChangedCallback != null) {
            manager.unregisterStatusListener(resourceId, statusChangedCallback);
        }
    }
}
