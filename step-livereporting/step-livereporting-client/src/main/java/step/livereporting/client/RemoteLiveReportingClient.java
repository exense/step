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


import step.reporting.impl.LiveMeasureSink;

public class RemoteLiveReportingClient implements LiveReportingClient {

    private final RestUploadingLiveMeasureSink liveMeasureSink;

    /**
     * @param reportingUrl the URL of the reporting endpoint for the specific reporting context
     */
    public RemoteLiveReportingClient(String reportingUrl) {
        liveMeasureSink = new RestUploadingLiveMeasureSink(reportingUrl);
    }

    @Override
    public LiveMeasureSink getLiveMeasureSink() {
        return liveMeasureSink;
    }

    @Override
    public void close() {
        liveMeasureSink.close();
    }
}
