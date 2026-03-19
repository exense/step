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
import step.core.reports.MetricSample;
import step.reporting.impl.LiveMetricSampleDestination;
import step.streaming.util.BatchProcessor;

import java.util.List;

public class RestUploadingLiveMetricSampleDestination implements LiveMetricSampleDestination {

    private static final Logger logger = LoggerFactory.getLogger(RestUploadingLiveMetricSampleDestination.class);
    private static final int DEFAULT_BATCH_SIZE = 500;
    private static final long DEFAULT_FLUSH_INTERVAL_MS = 5000;

    private final String endpointUrl;
    private final Client client;
    private final BatchProcessor<MetricSample> batchProcessor;

    public RestUploadingLiveMetricSampleDestination(String endpointUrl) {
        this(endpointUrl, DEFAULT_BATCH_SIZE, DEFAULT_FLUSH_INTERVAL_MS);
    }

    public RestUploadingLiveMetricSampleDestination(String endpointUrl, int batchSize, long flushIntervalMs) {
        this.endpointUrl = endpointUrl;
        this.client = createClient();
        this.batchProcessor = new BatchProcessor<>(batchSize, flushIntervalMs, this::sendMetricSamples, "livereporting-metric-samples-rest");
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
    public void accept(MetricSample metricSample) {
        batchProcessor.add(metricSample);
    }

    private void sendMetricSamples(List<MetricSample> metricSamples) {
        if (metricSamples == null || metricSamples.isEmpty()) {
            logger.debug("metricSamples is null or empty, skipping upload");
            return;
        }
        try (Response post = client.target(endpointUrl)
            .request()
            .post(Entity.entity(metricSamples, MediaType.APPLICATION_JSON_TYPE))) {
            // Make sure to always consume the response to avoid leak
            if (post.hasEntity()) {
                post.readEntity(String.class);
            }
            int status = post.getStatus();
            if (status != 204) {
                String msg = "Error while reporting metric samples. The live reporting service returned " + status;
                logger.error(msg);
            }
        }
    }

    @Override
    public void close() {
        batchProcessor.close();
        client.close();
    }
}
