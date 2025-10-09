package step.plugins.streaming;

import step.streaming.server.StreamingResourceManager;
import step.streaming.websocket.server.WebsocketServerEndpointSessionsHandler;
import step.streaming.websocket.server.WebsocketUploadEndpoint;

public class StepWebsocketUploadEndpoint extends WebsocketUploadEndpoint {
    public StepWebsocketUploadEndpoint(StreamingResourceManager manager, WebsocketServerEndpointSessionsHandler sessionsHandler) {
        super(manager, sessionsHandler);
    }
}
