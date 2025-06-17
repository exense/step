package step.streaming.websocket;

import step.streaming.StreamableResourcesManager;

public class StreamableResourcesEndpointsConfiguration {
    public final String uploadEndpoint;
    public final String downloadEndpoint;
    public final StreamableResourcesManager manager;

    private StreamableResourcesEndpointsConfiguration(String uploadEndpoint, String downloadEndpoint, StreamableResourcesManager manager) {
        this.uploadEndpoint = uploadEndpoint;
        this.downloadEndpoint = downloadEndpoint;
        this.manager = manager;
    }

    public static StreamableResourcesEndpointsConfiguration create(String controllerUrl, StreamableResourcesManager manager) {
        String wsUrl = controllerUrl.replaceFirst("^http", "ws");
        return new StreamableResourcesEndpointsConfiguration(wsUrl + StreamableResourcesUploadEndpoint.ENDPOINT_URL,
                wsUrl + StreamableResourcesDownloadEndpoint.ENDPOINT_URL_WITHOUT_ID,
                manager);
    }

    public String getDownloadEndpoint(String resourceId) {
        return downloadEndpoint + resourceId;
    }
}
