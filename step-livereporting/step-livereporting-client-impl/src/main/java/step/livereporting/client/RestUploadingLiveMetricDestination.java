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

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.metrics.Metric;
import step.core.metrics.MetricSnapshot;
import step.reporting.impl.LiveMetricDestination;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Sends {@link MetricSnapshot}s to the controller's live-reporting endpoint.
 * <p>
 * Metrics are registered via {@link #accept(Metric)}. This destination owns the flush
 * schedule: it calls {@link Metric#flush()} on each registered metric at a configured
 * interval and POSTs the resulting {@link MetricSnapshot}s to the controller.
 */
public class RestUploadingLiveMetricDestination implements LiveMetricDestination {

    private static final Logger logger = LoggerFactory.getLogger(RestUploadingLiveMetricDestination.class);
    private static final long DEFAULT_FLUSH_INTERVAL_MS = 5000;

    private final String endpointUrl;
    private final Client client;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentLinkedQueue<Metric> registeredMetrics = new ConcurrentLinkedQueue<>();

    public RestUploadingLiveMetricDestination(String endpointUrl) {
        this(endpointUrl, DEFAULT_FLUSH_INTERVAL_MS);
    }

    public RestUploadingLiveMetricDestination(String endpointUrl, long flushIntervalMs) {
        this.endpointUrl = endpointUrl;
        this.client = createClient();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "livereporting-metrics-rest");
            t.setDaemon(true);
            return t;
        });
        this.scheduler.scheduleAtFixedRate(this::scheduledFlushAndSend,
            flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
    }

    private Client createClient() {
        ClientConfig config = new ClientConfig();
        config.connectorProvider(new ApacheConnectorProvider());
        config.property(ClientProperties.CONNECT_TIMEOUT, 10_000);
        config.property(ClientProperties.READ_TIMEOUT, 30_000);
        config.register(JacksonFeature.class);
        return ClientBuilder.newClient(config);
    }

    @Override
    public void accept(Metric metric) {
        registeredMetrics.offer(metric);
    }

    private void scheduledFlushAndSend() {
        getAndSendMetricSnapshots();
    }

    private void getAndSendMetricSnapshots() {
        List<MetricSnapshot> snapshots = registeredMetrics.stream().map(Metric::flush).collect(Collectors.toList());
        if (!snapshots.isEmpty()) {
            sendMetrics(snapshots);
        }
    }

    private void sendMetrics(List<MetricSnapshot> metrics) {
        try (Response post = client.target(endpointUrl)
            .request()
            .post(Entity.entity(metrics, MediaType.APPLICATION_JSON_TYPE))) {
            if (post.hasEntity()) {
                post.readEntity(String.class);
            }
            int status = post.getStatus();
            if (status != 204) {
                logger.error("Error while reporting metrics. The live reporting service returned {}", status);
            }
        }
    }

    @Override
    public void close() {
        scheduler.shutdown();
        try {
            if (!this.scheduler.awaitTermination(5L, TimeUnit.SECONDS)) {
                this.scheduler.shutdownNow();
            }
        } catch (InterruptedException var2) {
            this.scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        // Final flush to capture any remaining accumulated metric values
        getAndSendMetricSnapshots();
        client.close();
    }
}
