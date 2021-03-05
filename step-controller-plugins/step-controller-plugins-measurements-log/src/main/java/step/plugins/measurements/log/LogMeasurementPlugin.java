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
package step.plugins.measurements.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.execution.ExecutionContext;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.plugins.measurements.AbstractMeasurementPlugin;
import step.plugins.measurements.GaugeCollector;
import step.plugins.measurements.GaugeCollectorRegistry;
import step.plugins.measurements.Measurement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin
@IgnoreDuringAutoDiscovery
public class LogMeasurementPlugin extends AbstractMeasurementPlugin {

	private static final Logger logger = LoggerFactory.getLogger(LogMeasurementPlugin.class);
	private static final Logger measurementLogger = LoggerFactory.getLogger("MeasurementLogger");
	ObjectMapper objectMapper;

	public LogMeasurementPlugin(){
		super();
		GaugeCollectorRegistry.getInstance().registerHandler(this);
		objectMapper = new ObjectMapper();
	}

	@Override
	protected void processMeasurements(List<Measurement> measurements, ExecutionContext executionContext) {
		List<?> rtmMeasurements = measurements;
		for (Object o: measurements) {
			try {
				measurementLogger.info(objectMapper.writeValueAsString(o));
			} catch (JsonProcessingException e) {
				logger.error("Measurement could not be formatted to json: " + o.toString(),e);
			}
		}
	}

	@Override
	public void processGauges(GaugeCollector collector, List<GaugeCollector.GaugeMetric> metrics) {
		for (GaugeCollector.GaugeMetric metric : metrics) {
			Map<String,Object> measurement = new HashMap<>();
			measurement.put("gauge_name", collector.getName());
			measurement.put(AbstractMeasurementPlugin.BEGIN,System.currentTimeMillis());
			String[] labels = collector.getLabels();
			for (int i=0; i < labels.length; i++) {
				measurement.put(labels[i],metric.labelsValue[i]);
			}
			measurement.put("value",metric.value);
			try {
				measurementLogger.info(objectMapper.writeValueAsString(measurement));
			} catch (JsonProcessingException e) {
				logger.error("Measurement could not be formatted to json: " + measurement.toString(),e);
			}
		}
	}
}
