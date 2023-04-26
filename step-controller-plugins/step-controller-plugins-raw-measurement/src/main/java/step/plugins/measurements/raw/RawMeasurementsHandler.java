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
import step.plugins.measurements.GaugeCollectorRegistry;
import step.plugins.measurements.MeasurementHandler;
import step.plugins.measurements.Measurement;

public class RawMeasurementsHandler implements MeasurementHandler {

	private static final Logger logger = LoggerFactory.getLogger(RawMeasurementsHandler.class);

	private final MeasurementAccessor accessor;

	public RawMeasurementsHandler(MeasurementAccessor accessor) {
		super();
		this.accessor = accessor;
		GaugeCollectorRegistry.getInstance().registerHandler(this);
	}

	public void processMeasurements(List<Measurement> measurements) {
		List<?> castedMeasurements = measurements;
		if (measurements.size()>0) {
			accessor.saveManyMeasurements((List<Object>) castedMeasurements);
		}
	}

	public void processGauges(List<Measurement> measurements) {
		processMeasurements(measurements);
	}

	public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext){
		executionContext.put(MeasurementAccessor.class,accessor);
	}
	public void afterExecutionEnd(ExecutionContext context) {}
}
