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
package step.plugins.measurements.prometheus;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.plugins.measurements.AbstractMeasurementPlugin;
import step.plugins.measurements.Measurement;

@Plugin
@IgnoreDuringAutoDiscovery
public class PrometheusPlugin extends AbstractMeasurementPlugin {

	private static final Logger logger = LoggerFactory.getLogger(PrometheusPlugin.class);

	private Map<String, Set<String[]>> labelsByExec;

	public PrometheusPlugin() {
		super();
		labelsByExec = new HashMap();
	}

	@Override
	public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
		super.initializeExecutionContext(executionEngineContext,executionContext);
		if (!labelsByExec.containsKey(executionContext.getExecutionId())) {
			labelsByExec.put(executionContext.getExecutionId(), new HashSet<>());
		}
	}

	@Override
	protected void processMeasurements(List<Measurement> measurements, ExecutionContext executionContext) {
		for (Measurement measurement : measurements) {
			String[] labels = {measurement.getExecId(), measurement.getName(), measurement.getType(),
					measurement.getStatus(), measurement.getPlanId(), measurement.getTaskId()};
			labelsByExec.get(executionContext.getExecutionId()).add(labels);
			PrometheusControllerPlugin.requestLatency.labels(labels).observe(measurement.getValue()/1000.0);
		}
	}

	@Override
	public void afterExecutionEnd(ExecutionContext context) {
		super.afterExecutionEnd(context);

		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		Runnable task = new Runnable() {
			public void run() {
				for (String[] labels : labelsByExec.remove(context.getExecutionId())) {
					PrometheusControllerPlugin.requestLatency.remove(labels);
				}
			}
		};
		int delay = 70;
		scheduler.schedule(task, delay, TimeUnit.SECONDS);
		scheduler.shutdown();
	}
}
