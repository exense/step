package step.streaming;

import jakarta.websocket.server.ServerEndpointConfig;
import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.grid.websockets.WebsocketServerEndpointSessionsHandler;
import step.streaming.impl.StreamableResourcesCollectionCatalogBackend;
import step.streaming.impl.StreamableResourcesFilesystemStorageBackend;
import step.streaming.websocket.StreamableResourcesDownloadEndpoint;
import step.streaming.websocket.StreamableResourcesUploadEndpoint;
import step.streaming.websocket.StreamableResourcesEndpointsConfiguration;

import java.io.File;

@Plugin
public class StreamableResourcesControllerPlugin extends AbstractControllerPlugin {
    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);
        File storageBaseDir = new File(System.getProperty("os.name").toLowerCase().contains("win") ? "C:/Temp/streaming-storage" : "/tmp/streaming-storage"); // FIXME

        StreamableResourcesManager manager = new StreamableResourcesManager(
                m -> new StreamableResourcesCollectionCatalogBackend(m, context),
                m -> new StreamableResourcesFilesystemStorageBackend(m, storageBaseDir, true));
        String controllerUrl = context.getConfiguration().getProperty("controller.url");
        // FIXME: quick-fix for dev machines
        controllerUrl = controllerUrl.replaceFirst(":4201", ":8080");
        StreamableResourcesEndpointsConfiguration endpointsConfiguration = StreamableResourcesEndpointsConfiguration.create(controllerUrl, manager);
        ServerEndpointConfig uploadConfig = StreamableResourcesUploadEndpoint.getEndpointConfig(endpointsConfiguration);
        context.getServiceRegistrationCallback().registerWebsocketEndpoint(uploadConfig);
        ServerEndpointConfig downloadConfig = StreamableResourcesDownloadEndpoint.getEndpointConfig(endpointsConfiguration);
        context.getServiceRegistrationCallback().registerWebsocketEndpoint(downloadConfig);
    }

    @Override
    public void serverStop(GlobalContext context) {
        // TODO: This is not the correct place really, but for now these are the only Websocket server endpoints.
        // Will have to think about a better place to put this.
        super.serverStop(context);
        // this will automatically take care of closing all active sessions
        WebsocketServerEndpointSessionsHandler.shutdown();
    }
}
