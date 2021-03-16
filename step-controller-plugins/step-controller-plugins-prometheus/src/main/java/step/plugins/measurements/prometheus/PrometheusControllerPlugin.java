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

import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.jetty.JettyStatisticsCollector;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.plugins.measurements.MeasurementPlugin;
import step.plugins.measurements.PrometheusCollectorRegistry;

@Plugin
public class PrometheusControllerPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(PrometheusControllerPlugin.class);
	public Histogram measurementHistogram;


	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		context.getServiceRegistrationCallback().registerService(PrometheusPluginServices.class);

		ch.exense.commons.app.Configuration stepProperties = context.getConfiguration();
		double[] buckets = null;
		String bucketsStr = stepProperties.getProperty("plugins.measurements.prometheus.buckets", "");
		String[] split = bucketsStr.split(",");
		if (split.length>0) {
			buckets = new double[split.length];
			for (int i=0; i<split.length;i++) {
				buckets[i] = Double.parseDouble(split[i]);
			}
		}
		measurementHistogram = PrometheusCollectorRegistry.getInstance().getOrCreateHistogram("step_node_duration_seconds",
				"step node duration in seconds.",buckets, "eId","name","type","status","planId","taskId");

		ServletContextHandler servletContext = new ServletContextHandler();
		servletContext.setContextPath("/metrics");
		servletContext.addServlet(new ServletHolder(new MetricsServlet()), "");
		context.getServiceRegistrationCallback().registerHandler(servletContext);

		//Start default JVM metrics
		DefaultExports.initialize();

		// Configure StatisticsHandler.
		StatisticsHandler stats = new StatisticsHandler();
		context.getServiceRegistrationCallback().registerHandler(stats);
		// Register collector.
		new JettyStatisticsCollector(stats).register();

		MeasurementPlugin.registerMeasurementHandlers(new PrometheusHandler(measurementHistogram));
	}

	@Override
	public void executionControllerDestroy(GlobalContext context) {

	}



}
