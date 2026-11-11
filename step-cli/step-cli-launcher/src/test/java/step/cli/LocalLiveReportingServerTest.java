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

import org.junit.Assert;
import org.junit.Test;
import step.core.execution.ExecutionEngine;
import step.core.reports.Measure;
import step.livereporting.LiveReportingContext;
import step.livereporting.LiveReportingContexts;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LocalLiveReportingServerTest {

    @Test
    public void receivesTheLiveMeasures() throws Exception {
        try (LocalLiveReportingServer server = new LocalLiveReportingServer();
             ExecutionEngine executionEngine = ExecutionEngine.builder().withPlugin(server.getExecutionEnginePlugin())
                 .withPluginsFromClasspath().build()) {
            LiveReportingContext context = executionEngine.getExecutionEngineContext().require(LiveReportingContexts.class).createNewContext();
            List<Measure> received = new CopyOnWriteArrayList<>();
            context.registerMeasureListener(received::addAll);

            HttpResponse<String> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                .uri(URI.create(server.getUrl() + "/rest/live-reporting/" + context.id + "/measures"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("[{\"name\":\"Transaction 1\",\"duration\":10,\"begin\":0}]"))
                .build(), HttpResponse.BodyHandlers.ofString());

            Assert.assertEquals(response.body(), 204, response.statusCode());
            Assert.assertEquals(1, received.size());
            Assert.assertEquals("Transaction 1", received.get(0).getName());
        }
    }
}
