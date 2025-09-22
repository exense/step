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

package step.livereporting;

import step.core.reports.Measure;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LiveReportingContext {

    /**
     * The unique ID of this context
     */
    public final String id = UUID.randomUUID().toString();
    private final List<LiveReportingContextListener> listeners = new ArrayList<>();
    private final String injectionContextUrl;

    public LiveReportingContext(String injectionContextUrl) {
        this.injectionContextUrl = injectionContextUrl + "/" + id;
    }

    /**
     * @return the base URL of the reporting endpoint for this specific context
     */
    public String getReportingUrl() {
        return injectionContextUrl;
    }

    public void registerListener(LiveReportingContextListener contextListener) {
        listeners.add(contextListener);
    }

    public void onMeasuresReceived(List<Measure> measures) {
        for (LiveReportingContextListener listener : listeners) {
            listener.accept(measures);
        }
    }
}
