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
    private final String reportingContextUrl;
    private final Client client;

    public RestUploadingLiveMeasureSink(String reportingContextUrl) {
        this.reportingContextUrl = reportingContextUrl;
        client = ClientBuilder.newClient().register(JacksonFeature.class);
    }

    @Override
    public void accept(Measure measure) {
        // TODO Implement batching
        try (Response post = client.target(reportingContextUrl + "/measures").request().post(Entity.entity(List.of(measure), MediaType.APPLICATION_JSON_TYPE))) {
            int status = post.getStatus();
            if(status != 204) {
                // TODO improve error handling
                throw new RuntimeException("Error while reporting measures. The live reporting service returned " + status);
            }
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
