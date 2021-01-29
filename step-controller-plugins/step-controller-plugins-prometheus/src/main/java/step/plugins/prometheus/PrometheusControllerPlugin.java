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
package step.plugins.prometheus;

import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.jetty.JettyStatisticsCollector;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.ExecutionEnginePlugin;

@Plugin
public class PrometheusControllerPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(PrometheusControllerPlugin.class);
	public static final Histogram requestLatency = Histogram.build()
			.name("step_node_duration_seconds").help("step node duration in seconds.")
			.labelNames("eId","name","type","status").linearBuckets(0.1D,0.1D,100).register();


	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		context.getServiceRegistrationCallback().registerService(PrometheusPluginServices.class);

		ch.exense.commons.app.Configuration stepProperties = context.getConfiguration();

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

		//TODO Would create the database and table here and any indexes here if required

	}

	@Override
	public void executionControllerDestroy(GlobalContext context) {

	}

	@Override
	public ExecutionEnginePlugin getExecutionEnginePlugin() {
		return new PrometheusPlugin();
	}

}
