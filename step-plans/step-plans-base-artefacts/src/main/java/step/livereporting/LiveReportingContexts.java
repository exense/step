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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.reports.Measure;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class LiveReportingContexts {

    private static final Logger logger = LoggerFactory.getLogger(LiveReportingContexts.class);
    private final Map<String, LiveReportingContext> contexts = new ConcurrentHashMap<>();

    private final String injectionUrlTemplate;

    public LiveReportingContexts(String injectionUrlTemplate) {
        this.injectionUrlTemplate = injectionUrlTemplate;
    }

    public void onMeasuresReceived(String contextHandle, List<Measure> measures) {
        LiveReportingContext context = getNonNullContext(contextHandle);
        try {
            context.onMeasuresReceived(measures);
        } catch (Exception e) {
            logger.error("Error while dispatching {} measures for context handle {}", measures.size(), contextHandle, e);
        }
    }

    private LiveReportingContext getNonNullContext(String contextHandle) {
        return Objects.requireNonNull(contexts.get(contextHandle), "Context with id " + contextHandle + " not found");
    }

    public void unregister(String liveMeasureContextHandle) {
        contexts.remove(liveMeasureContextHandle);
    }

    public LiveReportingContext createReportingContext() {
        LiveReportingContext context = new LiveReportingContext(injectionUrlTemplate);
        contexts.put(context.id, context);
        return context;
    }
}
