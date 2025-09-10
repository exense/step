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
import step.core.controller.StepControllerPlugin;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.ExecutionEnginePlugin;
import step.resources.ResourceManagerControllerPlugin;
import step.resources.StreamingResourceContentProvider;
import step.streaming.common.StreamingResourceUploadContexts;
import step.streaming.server.FilesystemStreamingResourcesStorageBackend;
import step.streaming.server.StreamingResourceManager;
import step.streaming.server.URITemplateBasedReferenceProducer;
import step.streaming.websocket.server.DefaultWebsocketServerEndpointSessionsHandler;
import step.streaming.websocket.server.WebsocketDownloadEndpoint;
import step.streaming.websocket.server.WebsocketServerEndpointSessionsHandler;
import step.streaming.websocket.server.WebsocketUploadEndpoint;

import java.io.File;
import java.net.URI;
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
    private StepStreamingResourceManager manager;
    private String websocketBaseUrl;

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);

        String controllerUrl = StepControllerPlugin.getControllerUrl(context.getConfiguration(), true, true);
        // We need the websocket variant
        URI websocketBaseUri = URI.create(controllerUrl.replaceFirst("^http", "ws"));

        websocketBaseUrl = websocketBaseUri.toString();

        File resourcesRootDir = new File(ResourceManagerControllerPlugin.getResourceDir(context.getConfiguration()));
        File storageBaseDir = new File(resourcesRootDir, "streamedAttachment");

        FilesystemStreamingResourcesStorageBackend storage = new FilesystemStreamingResourcesStorageBackend(storageBaseDir);
        StreamingResourceCollectionCatalogBackend catalog = new StreamingResourceCollectionCatalogBackend(context);
        URITemplateBasedReferenceProducer referenceProducer = new URITemplateBasedReferenceProducer(websocketBaseUri, DOWNLOAD_PATH, DOWNLOAD_PARAMETER_NAME);
        manager = new StepStreamingResourceManager(context, catalog, storage, referenceProducer, uploadContexts);

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
                Object session = httpSession.getAttribute(ATTRIBUTE_STEP_SESSION);
                if (session != null) {
                    config.getUserProperties().put(ATTRIBUTE_STEP_SESSION, httpSession.getAttribute("session"));
                }
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
            public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext executionEngineContext) {
                // required by AP reporting for fetching attachments
                executionEngineContext.put(StreamingResourceContentProvider.class, manager);
            }

            @Override
            public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
                // Makes streaming available to the execution
                executionContext.put(StreamingResourceUploadContexts.class, uploadContexts);
                executionContext.put(StreamingConstants.AttributeNames.WEBSOCKET_BASE_URL, websocketBaseUrl);
                executionContext.put(StreamingConstants.AttributeNames.WEBSOCKET_UPLOAD_PATH, UPLOAD_PATH);
            }



            @Override
            public void afterExecutionEnd(ExecutionContext context) {
                // unregisters the execution with manager (registration is on-demand,
                // but execution end needs to be signaled explicitly)
                manager.unregisterExecution(context.getExecutionId());
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
