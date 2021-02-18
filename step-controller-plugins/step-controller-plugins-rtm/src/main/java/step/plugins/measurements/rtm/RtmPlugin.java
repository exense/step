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
package step.plugins.measurements.rtm;

import java.util.*;

import org.rtm.commons.MeasurementAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.execution.ExecutionContext;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.plugins.measurements.AbstractMeasurementPlugin;
import step.plugins.measurements.Measurement;

@Plugin
@IgnoreDuringAutoDiscovery
public class RtmPlugin extends AbstractMeasurementPlugin {

	private static final Logger logger = LoggerFactory.getLogger(RtmPlugin.class);

	private final MeasurementAccessor accessor;

	public RtmPlugin(MeasurementAccessor accessor) {
		super();
		this.accessor = accessor;
	}

	@Override
	protected void processMeasurements(List<Measurement> measurements, ExecutionContext executionContext) {
		List<?> rtmMeasurements = measurements;
		if (measurements.size()>0) {
			accessor.saveManyMeasurements((List<Object>) rtmMeasurements);
		}
	}
}
