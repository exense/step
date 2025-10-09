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
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.reports.Measure;
import step.reporting.impl.LiveMeasureSink;

import java.util.List;

public class RestUploadingLiveMeasureSink implements LiveMeasureSink {

    private static final Logger logger = LoggerFactory.getLogger(RestUploadingLiveMeasureSink.class);
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final long DEFAULT_FLUSH_INTERVAL_MS = 1000;

    private final String reportingContextUrl;
    private final Client client;
    private final BatchProcessor<Measure> batchProcessor;

    public RestUploadingLiveMeasureSink(String reportingContextUrl) {
        this(reportingContextUrl, DEFAULT_BATCH_SIZE, DEFAULT_FLUSH_INTERVAL_MS);
    }

    public RestUploadingLiveMeasureSink(String reportingContextUrl, int batchSize, long flushIntervalMs) {
        this.reportingContextUrl = reportingContextUrl;
        this.client = ClientBuilder.newClient().register(JacksonFeature.class);
        this.batchProcessor = new BatchProcessor<>(batchSize, flushIntervalMs, this::sendMeasures, "livereporting-measures-rest");
    }

    @Override
    public void accept(Measure measure) {
        batchProcessor.add(measure);
    }

    private void sendMeasures(List<Measure> measures) {
        if (measures == null || measures.isEmpty()) {
            logger.debug("measures is null or empty, skipping upload");
            return;
        }
        // the final URL corresponds to the one defined in LiveReportingServices (step-controller-base-plugins)
        try (Response post = client.target(reportingContextUrl + "/measures")
                .request()
                .post(Entity.entity(measures, MediaType.APPLICATION_JSON_TYPE))) {
            int status = post.getStatus();
            if (status != 204) {
                String msg = "Error while reporting measures. The live reporting service returned " + status;
                logger.error(msg);
                // FIXME: Do we really want to throw an exception here?
                throw new RuntimeException("Error while reporting measures. The live reporting service returned " + status);
            }
        }
    }

    @Override
    public void close() {
        batchProcessor.close();
        client.close();
    }
}
