/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.cli;

import ch.exense.commons.io.FileHelper;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.AbstractContext;
import step.core.GlobalContext;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.engine.plugins.ExecutionEnginePlugin;
import step.livereporting.LiveReportingContexts;
import step.livereporting.LiveReportingPlugin;
import step.plugins.livereporting.LiveReportingServices;
import step.plugins.streaming.QuotaLimits;
import step.plugins.streaming.StepStreamingResourceManager;
import step.plugins.streaming.StreamingResourceAccessor;
import step.plugins.streaming.StreamingResourceCollectionCatalogBackend;
import step.plugins.streaming.StreamingResourcesControllerPlugin;
import step.plugins.streaming.StreamingUploadsExecutionPlugin;
import step.streaming.common.StreamingResourceUploadContexts;
import step.streaming.server.FilesystemStreamingResourcesStorageBackend;
import step.streaming.server.URITemplateBasedReferenceProducer;
import step.streaming.websocket.server.DefaultWebsocketServerEndpointSessionsHandler;
import step.streaming.websocket.server.WebsocketServerEndpointSessionsHandler;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;

/**
 * The server the agents of a local execution report to while the keywords run, as they do to the controller of a
 * Step platform: the keywords upload the files they stream to it, and send it their live measures and metrics. It
 * listens on the loopback interface only, the agents running on this machine.
 * <p>
 * Its {@link #getExecutionEnginePlugin() plugin} makes it the storage of all the attachments of the executions, the
 * streamed ones as the others, as the controller does. They are kept in a temporary folder deleted once the server
 * is closed, which the {@link AttachmentExporter} writes them to the report folder from before. The live measures and
 * metrics are received and dropped, a local execution having no use for them.
 */
public class LocalLiveReportingServer implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(LocalLiveReportingServer.class);

    private static final String LOOPBACK_ADDRESS = "127.0.0.1";

    private final File storageDirectory;
    private final StreamingResourceUploadContexts uploadContexts = new StreamingResourceUploadContexts();
    private final WebsocketServerEndpointSessionsHandler sessionsHandler = new DefaultWebsocketServerEndpointSessionsHandler();
    private final StepStreamingResourceManager manager;
    private final Server server;
    private final String url;
    // What the controller services find in the global context of the controller, the live reporting contexts once the
    // execution engine has been built, the live reporting plugin creating them
    private final GlobalContext serverContext = new GlobalContext();

    public LocalLiveReportingServer() throws IOException {
        storageDirectory = Files.createTempDirectory("step-cli-attachments-").toFile();
        try {
            server = new Server();
            ServerConnector connector = new ServerConnector(server);
            connector.setHost(LOOPBACK_ADDRESS);
            // Any free port
            connector.setPort(0);
            server.addConnector(connector);

            manager = new StepStreamingResourceManager(
                new StreamingResourceCollectionCatalogBackend(new StreamingResourceAccessor(new InMemoryCollection<>())),
                new FilesystemStreamingResourcesStorageBackend(storageDirectory),
                new URITemplateBasedReferenceProducer(URI.create("ws://" + LOOPBACK_ADDRESS),
                    StreamingResourcesControllerPlugin.DOWNLOAD_PATH, StreamingResourcesControllerPlugin.DOWNLOAD_PARAMETER_NAME),
                uploadContexts, null, null, QuotaLimits.UNLIMITED);

            ResourceConfig resourceConfig = new ResourceConfig();
            resourceConfig.register(JacksonFeature.class);
            resourceConfig.register(new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(serverContext).to(AbstractContext.class);
                    bind(serverContext).to(GlobalContext.class);
                }
            });
            resourceConfig.register(LiveReportingServices.class);
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            context.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/rest/*");
            JakartaWebSocketServletContainerInitializer.configure(context, (servletContext, container) ->
                container.addEndpoint(StreamingResourcesControllerPlugin.makeUploadConfig(manager, sessionsHandler)));
            server.setHandler(context);
            server.start();

            url = "http://" + LOOPBACK_ADDRESS + ":" + connector.getLocalPort();
            logger.debug("Live reporting server listening on {}", url);
        } catch (Exception e) {
            FileHelper.deleteFolder(storageDirectory);
            throw new IOException("Unable to start the server receiving the attachments streamed by the keywords", e);
        }
    }

    /**
     * @return the URL the agents report to
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the plugin making the executions of the engine report to this server
     */
    public ExecutionEnginePlugin getExecutionEnginePlugin() {
        return new LocalLiveReportingPlugin();
    }

    @Override
    public void close() {
        try {
            sessionsHandler.shutdown();
            server.stop();
        } catch (Exception e) {
            logger.warn("Unable to stop the live reporting server", e);
        } finally {
            if (!FileHelper.deleteFolder(storageDirectory)) {
                logger.warn("Unable to delete the attachments folder {}", storageDirectory);
            }
        }
    }

    /**
     * Depends on the {@link LiveReportingPlugin} for the live reporting contexts it creates
     */
    @Plugin(dependencies = LiveReportingPlugin.class)
    @IgnoreDuringAutoDiscovery
    private class LocalLiveReportingPlugin extends StreamingUploadsExecutionPlugin {

        private LocalLiveReportingPlugin() {
            super(manager, uploadContexts, url);
        }

        @Override
        public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext executionEngineContext) {
            executionEngineContext.setAttachmentStorage(manager);
            serverContext.put(LiveReportingContexts.class, executionEngineContext.require(LiveReportingContexts.class));
        }
    }
}
