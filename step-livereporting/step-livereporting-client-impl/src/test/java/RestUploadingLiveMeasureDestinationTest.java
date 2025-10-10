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

import org.junit.Ignore;
import org.junit.Test;
import step.core.reports.Measure;
import step.livereporting.client.RestUploadingLiveMeasureDestination;

import java.util.Map;

public class RestUploadingLiveMeasureDestinationTest {

    @Ignore
    @Test
    public void accept() {
        // TODO finalize or remove this test
        RestUploadingLiveMeasureDestination client = new RestUploadingLiveMeasureDestination("http://localhost:8080/reporting/");
        client.accept(new Measure("test", 10, System.currentTimeMillis(), Map.of()));
    }
}