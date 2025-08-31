package step.plugins.streaming;

import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import step.core.access.User;
import step.streaming.websocket.server.WebsocketDownloadEndpoint;
import step.streaming.websocket.server.WebsocketServerEndpointSessionsHandler;

import java.util.Objects;

import static step.plugins.streaming.StepStreamingResourceManager.ATTRIBUTE_STEP_SESSION;

public class StepWebsocketDownloadEndpoint extends WebsocketDownloadEndpoint {
    public StepWebsocketDownloadEndpoint(StepStreamingResourceManager manager, WebsocketServerEndpointSessionsHandler sessionsHandler, String resourceIdParameterName) {
        super(manager, sessionsHandler, resourceIdParameterName);
    }

    // simply cast the protected field to the correct instance class
    private StepStreamingResourceManager getManager() {
        return (StepStreamingResourceManager) manager;
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        try {
            @SuppressWarnings("unchecked")
            step.framework.server.Session<User> stepSession = (step.framework.server.Session<User>) config.getUserProperties().get(ATTRIBUTE_STEP_SESSION);
            if (stepSession == null) {
                throw new IllegalStateException("No Step session found");
            }

            // We do permissions check before even calling the super method to initialize the session.
            getManager().checkDownloadPermission(Objects.requireNonNull(session.getPathParameters().get(resourceIdParameterName)), stepSession);
            super.onOpen(session, config);
        } catch (Exception e) {
            // We could just throw and let the framework handle the exceptions, but then the error messages become almost useless:
            // "WebSocket connection closed. Code: 1011, Reason: org.eclipse.jetty.websocket.core.exception.WebSocketException: StepWebsocketDownloadEndpoint OPEN method error: Access deni"
            // So we just close the session immediately with a proper message, which basically has the same effect.
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, e.getMessage()));
            } catch (Exception e2) {
                throw new RuntimeException(e);
            }
        }
    }
}
