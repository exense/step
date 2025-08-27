package step.plugins.streaming;

import jakarta.servlet.http.HttpSession;
import jakarta.websocket.Endpoint;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.constants.StreamingConstants;
import step.core.GlobalContext;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.ExecutionEnginePlugin;
import step.resources.ResourceManagerControllerPlugin;
import step.streaming.common.StreamingResourceUploadContexts;
import step.streaming.server.*;
import step.streaming.websocket.server.DefaultWebsocketServerEndpointSessionsHandler;
import step.streaming.websocket.server.WebsocketDownloadEndpoint;
import step.streaming.websocket.server.WebsocketServerEndpointSessionsHandler;
import step.streaming.websocket.server.WebsocketUploadEndpoint;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import static step.plugins.streaming.StepStreamingResourceManager.ATTRIBUTE_STEP_SESSION;

@Plugin(dependencies = ObjectHookControllerPlugin.class)
public class StreamingResourcesControllerPlugin extends AbstractControllerPlugin {
    public static final String UPLOAD_PATH = WebsocketUploadEndpoint.DEFAULT_ENDPOINT_URL;
    public static final String DOWNLOAD_PATH = WebsocketDownloadEndpoint.DEFAULT_ENDPOINT_URL;
    public static final String DOWNLOAD_PARAMETER_NAME = WebsocketDownloadEndpoint.DEFAULT_PARAMETER_NAME;

    private static final Logger logger = LoggerFactory.getLogger(StreamingResourcesControllerPlugin.class);
    private final WebsocketServerEndpointSessionsHandler sessionsHandler = new DefaultWebsocketServerEndpointSessionsHandler();
    private final StreamingResourceUploadContexts uploadContexts = new StreamingResourceUploadContexts();
    private String websocketBaseUrl;

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);

        // FIXME: this is quick and dirty for now, so it runs both on dev machines (OS/EE) and (hopefully) the grid.
        // There needs to be some better logic.
        int controllerPort = context.getConfiguration().getPropertyAsInteger("port", 8080);
        String controllerUrl = context.getConfiguration().getProperty("controller.url");
        if (controllerUrl == null) {
            controllerUrl = "http://localhost:" + controllerPort;
            logger.warn("controller.url is not set in step.properties, unable to determine correct URL for streaming services. Will use {} instead, but this will not be accessible remotely.", controllerUrl);
        } else if (controllerUrl.equals("http://localhost:4201")) {
            // adjust (only for websockets, not globally)
            controllerUrl = "http://localhost:" + controllerPort;
        }
        // remove trailing slash if present
        if (controllerUrl.endsWith("/")) {
            controllerUrl = controllerUrl.substring(0, controllerUrl.length() - 1);
        }
        URI websocketBaseUri = URI.create(controllerUrl.replaceFirst("^http", "ws"));

        websocketBaseUrl = websocketBaseUri.toString();

        File resourcesRootDir = new File(ResourceManagerControllerPlugin.getResourceDir(context.getConfiguration()));
        File storageBaseDir = new File(resourcesRootDir, "streamedAttachment");

        FilesystemStreamingResourcesStorageBackend storage = new FilesystemStreamingResourcesStorageBackend(storageBaseDir);
        StreamingResourceCollectionCatalogBackend catalog = new StreamingResourceCollectionCatalogBackend(context);
        URITemplateBasedReferenceProducer referenceProducer = new URITemplateBasedReferenceProducer(websocketBaseUri, DOWNLOAD_PATH, DOWNLOAD_PARAMETER_NAME);
        StepStreamingResourceManager manager = new StepStreamingResourceManager(context, catalog, storage, referenceProducer, uploadContexts);

        context.put(StepStreamingResourceManager.class, manager);
        context.getServiceRegistrationCallback().registerService(StreamingResourceServices.class);

        context.getServiceRegistrationCallback().registerWebsocketEndpoint(makeUploadConfig(manager));
        context.getServiceRegistrationCallback().registerWebsocketEndpoint(makeDownloadConfig(manager));
        logger.info("Streaming Websockets plugin started, upload/download URLs: {}, {}", websocketBaseUri + UPLOAD_PATH, websocketBaseUri + DOWNLOAD_PATH);
    }

    private static class EndpointConfigurator extends ServerEndpointConfig.Configurator {

        private final Supplier<Endpoint> constructor;
        public EndpointConfigurator(Supplier<Endpoint> constructor) {
            this.constructor = constructor;
        }

        @Override
        public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
            HttpSession httpSession = (HttpSession) request.getHttpSession();
            if (httpSession != null) {
                config.getUserProperties().put(ATTRIBUTE_STEP_SESSION, httpSession.getAttribute("session"));
            }
        }

        @Override
        public <T> T getEndpointInstance(Class<T> endpointClass) {
            return endpointClass.cast(constructor.get());
        }
    }

    private ServerEndpointConfig makeUploadConfig(StreamingResourceManager manager) {
        return ServerEndpointConfig.Builder.create(StepWebsocketUploadEndpoint.class, UPLOAD_PATH)
                .configurator(new EndpointConfigurator(() -> new StepWebsocketUploadEndpoint(manager, sessionsHandler)))
                .build();
    }

    private ServerEndpointConfig makeDownloadConfig(StepStreamingResourceManager manager) {
        return ServerEndpointConfig.Builder.create(StepWebsocketDownloadEndpoint.class, DOWNLOAD_PATH)
                .configurator(new EndpointConfigurator(() -> new StepWebsocketDownloadEndpoint(manager, sessionsHandler, DOWNLOAD_PARAMETER_NAME)))
                .build();
    }

    @Override
    public ExecutionEnginePlugin getExecutionEnginePlugin() {
        return new AbstractExecutionEnginePlugin() {
            @Override
            public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
                executionContext.put(StreamingResourceUploadContexts.class, uploadContexts);
                executionContext.put(StreamingConstants.AttributeNames.WEBSOCKET_BASE_URL, websocketBaseUrl);
                executionContext.put(StreamingConstants.AttributeNames.WEBSOCKET_UPLOAD_PATH, UPLOAD_PATH);
            }
        };
    }

    @Override
    public void serverStop(GlobalContext context) {
        // TODO: This is not the correct place really, but for now these are the only Websocket server endpoints.
        // Will have to think about a better place to put this, potentially simply in a separate plugin
        // This will automatically take care of closing all active websocket sessions on shutdown.
        sessionsHandler.shutdown();
        super.serverStop(context);
    }
}
