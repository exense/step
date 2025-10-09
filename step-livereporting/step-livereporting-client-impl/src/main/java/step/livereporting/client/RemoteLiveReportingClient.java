/*
 * Copyright (C) 2025, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

package step.livereporting.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.constants.LiveReportingConstants;
import step.reporting.impl.LiveMeasureSink;
import step.streaming.client.upload.StreamingUploadProvider;
import step.streaming.common.StreamingResourceUploadContext;
import step.streaming.websocket.client.upload.WebsocketUploadProvider;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

public class RemoteLiveReportingClient implements LiveReportingClient {

    private static final Logger logger = LoggerFactory.getLogger(RemoteLiveReportingClient.class);

    private final RestUploadingLiveMeasureSink liveMeasureSink;
    private final StreamingUploadProvider streamingUploadProvider;

    public RemoteLiveReportingClient(Map<String, String> contextProperties, Map<String, String> agentProperties, ExecutorService executorService, AtomicReference<Object> websocketContainer) {
        // We currently only support Websocket uploads; if this changes in the future, here is the place to modify the logic.
        String streamingUploadsContextId = contextProperties.get(StreamingResourceUploadContext.PARAMETER_NAME);
        if (streamingUploadsContextId != null) {
            // we need to use a singleton instance of WebSocketContainer (for all clients),
            // so initialize it once if not yet set
            if (websocketContainer.get() == null) {
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (websocketContainer) {
                    if (websocketContainer.get() == null) {
                        websocketContainer.set(WebsocketUploadProvider.instantiateWebSocketContainer());
                    }
                }
            }
            URI websocketUploadUri = getWebsocketUploadUri(contextProperties, agentProperties, streamingUploadsContextId);
            streamingUploadProvider = new WebsocketUploadProvider(websocketContainer.get(), executorService, websocketUploadUri);
        } else {
            // API liveReporting knows how to handle null values
            streamingUploadProvider = null;
        }
        String liveReportingUrl = contextProperties.get(LiveReportingConstants.LIVEREPORTING_CONTEXT_URL);
        if (liveReportingUrl != null) {
            liveMeasureSink = new RestUploadingLiveMeasureSink(liveReportingUrl);
        } else {
            // API liveReporting knows how to handle null values
            liveMeasureSink = null;
        }
    }

    @Override
    public LiveMeasureSink getLiveMeasureSink() {
        return liveMeasureSink;
    }

    @Override
    public StreamingUploadProvider getStreamingUploadProvider() {
        return streamingUploadProvider;
    }

    private URI getWebsocketUploadUri(Map<String, String> properties, Map<String, String> agentProperties, String uploadContextId) {
        String host; // actually contains scheme, hostname, and potentially port.
        // If present, agent-side configuration overrides the default value, but both agentProperties or the value might be undefined.
        String agentConfUrl = Optional.ofNullable(agentProperties)
                .map(m -> m.get("step.reporting.url"))
                .orElse(null);
        if (agentConfUrl != null) {
            // just a sanity check really
            if (!agentConfUrl.matches("^https?://.+")) {
                throw new IllegalArgumentException("Invalid URL in 'step.reporting.url' (agent-side configuration): " + agentConfUrl);
            }
            // switch from http to websocket by replacing prefix
            host = agentConfUrl.replaceAll("^http", "ws");
        } else {
            // The controller defines a default URL, derived from controller.url in step.properties.
            // This is always defined, and already has the correct Websocket prefix.
            host = properties.get(LiveReportingConstants.STREAMING_WEBSOCKET_BASE_URL);
        }
        // Strip trailing slashes, just in case there are any (normally not expected)
        while (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        String path = properties.get(LiveReportingConstants.STREAMING_WEBSOCKET_UPLOAD_PATH);
        // Strip leading slashes, just in case
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        URI uri = URI.create(String.format("%s/%s?%s=%s", host, path, StreamingResourceUploadContext.PARAMETER_NAME, uploadContextId));
        if (logger.isDebugEnabled()) {
            // Don't log context id, it's "semi-secret"
            logger.debug("Effective URL for Websocket uploads: {}", String.format("%s/%s", host, path));
        }
        return uri;
    }

}
