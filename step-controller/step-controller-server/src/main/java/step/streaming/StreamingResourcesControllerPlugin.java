package step.streaming;

import jakarta.websocket.server.ServerEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.streaming.server.*;
import step.streaming.websocket.server.DefaultWebsocketServerEndpointSessionsHandler;
import step.streaming.websocket.server.WebsocketDownloadEndpoint;
import step.streaming.websocket.server.WebsocketServerEndpointSessionsHandler;
import step.streaming.websocket.server.WebsocketUploadEndpoint;

import java.io.File;
import java.net.URI;

@Plugin
public class StreamingResourcesControllerPlugin extends AbstractControllerPlugin {
    private static final Logger logger = LoggerFactory.getLogger(StreamingResourcesControllerPlugin.class);
    private final WebsocketServerEndpointSessionsHandler sessionsHandler = DefaultWebsocketServerEndpointSessionsHandler.getInstance();

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);
        String websocketBaseUrl = context.getConfiguration().getProperty("controller.url");
        websocketBaseUrl= websocketBaseUrl.replaceFirst("^http", "ws");
        websocketBaseUrl = websocketBaseUrl.replaceFirst(":4201", ":8080");
        URI websocketBaseUri = URI.create(websocketBaseUrl);

        String downloadPath = WebsocketDownloadEndpoint.DEFAULT_ENDPOINT_URL;
        String idParameterName = WebsocketDownloadEndpoint.DEFAULT_PARAMETER_NAME;
        String uploadPath = WebsocketUploadEndpoint.DEFAULT_ENDPOINT_URL;

        File storageBaseDir = new File(System.getProperty("os.name").toLowerCase().contains("win") ? "C:/Temp/streaming-storage" : "/tmp/streaming-storage"); // FIXME

        StreamingResourcesStorageBackend storage = new FilesystemStreamingResourcesStorageBackend(storageBaseDir);
        StreamingResourcesCatalogBackend catalog = new StreamingResourceCollectionCatalogBackend(context);
        StreamingResourceReferenceMapper mapper = new DefaultStreamingResourceReferenceMapper(websocketBaseUri, downloadPath, idParameterName);
        StreamingResourceManager manager = new DefaultStreamingResourceManager(catalog, storage, mapper);

        context.getServiceRegistrationCallback().registerWebsocketEndpoint(makeUploadConfig(manager, uploadPath));
        context.getServiceRegistrationCallback().registerWebsocketEndpoint(makeDownloadConfig(manager, downloadPath, idParameterName));
        logger.info("{} started, upload/download URLs: {}, {}", this.getClass().getSimpleName(), websocketBaseUrl + uploadPath, websocketBaseUrl + downloadPath);
    }

    private ServerEndpointConfig makeUploadConfig(StreamingResourceManager manager, String uploadPath) {
        return ServerEndpointConfig.Builder.create(WebsocketUploadEndpoint.class, uploadPath)
                .configurator(new ServerEndpointConfig.Configurator() {
                    @Override
                    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                        return endpointClass.cast(new WebsocketUploadEndpoint(manager, sessionsHandler));
                    }
                })
                .build();
    }

    private ServerEndpointConfig makeDownloadConfig(StreamingResourceManager manager, String downloadPath, String idParameterName) {
        return ServerEndpointConfig.Builder.create(WebsocketDownloadEndpoint.class, downloadPath)
                .configurator(new ServerEndpointConfig.Configurator() {
                    @Override
                    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                        return endpointClass.cast(new WebsocketDownloadEndpoint(manager, sessionsHandler, idParameterName));
                    }
                })
                .build();
    }

    @Override
    public void serverStop(GlobalContext context) {
        // TODO: This is not the correct place really, but for now these are the only Websocket server endpoints.
        // Will have to think about a better place to put this, potentially simply in a separate plugin
        // This will automatically take care of closing all active websocket sessions.
        sessionsHandler.shutdown();
        super.serverStop(context);
    }
}
