/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
package step.plugins.measurements.raw;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.plugins.measurements.MetricSamplerRegistry;
import step.plugins.measurements.MetricHeartbeatRegistry;
import step.plugins.measurements.SamplesHandler;
import step.plugins.measurements.Measurement;
import step.plugins.measurements.ExecutionMetricSample;

public class RawSamplesHandler implements SamplesHandler {

    private static final Logger logger = LoggerFactory.getLogger(RawSamplesHandler.class);

    private final MeasurementAccessor accessor;
    private final MetricSampleAccessor metricSampleAccessor;

    public RawSamplesHandler(MeasurementAccessor accessor, MetricSampleAccessor metricSampleAccessor) {
        super();
        this.accessor = accessor;
        this.metricSampleAccessor = metricSampleAccessor;
        MetricSamplerRegistry.getInstance().registerHandler(this);
        MetricHeartbeatRegistry.getInstance().registerHandler(this);
    }

    @Override
    public void processMeasurements(List<Measurement> measurements) {
        List<?> castedMeasurements = measurements;
        if (measurements.size() > 0) {
            accessor.saveManyMeasurements((List<Object>) castedMeasurements);
        }
    }

    @Override
    public void processMetrics(List<ExecutionMetricSample> metrics) {
        if (metrics != null && ! metrics.isEmpty()) {
            metricSampleAccessor.save(metrics);
        }
    }

    public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
        executionContext.put(MeasurementAccessor.class, accessor);
    }

    public void afterExecutionEnd(ExecutionContext context) {
    }
}
