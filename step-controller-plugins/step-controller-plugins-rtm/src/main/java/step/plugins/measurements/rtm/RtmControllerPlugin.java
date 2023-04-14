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

import java.io.File;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import ch.exense.commons.app.Configuration;
import org.bson.Document;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.rtm.commons.MeasurementAccessor;

import org.rtm.commons.RtmContext;
import org.rtm.jetty.JettyStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.plugins.measurements.MeasurementPlugin;

@Plugin
public class RtmControllerPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(RtmControllerPlugin.class);

	private MeasurementAccessor accessor;

	private RtmContext rtmContext;

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		context.getServiceRegistrationCallback().registerService(RtmPluginServices.class);
		String fileName = "rtm.properties";
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
		File file = File.createTempFile(fileName + "-" + UUID.randomUUID(), fileName.substring(fileName.lastIndexOf(".")));
		Files.copy(inputStream, file.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
		file.deleteOnExit();

		Configuration rtmConfig = new Configuration(file);
		Configuration stepConfig = context.getConfiguration();
		if(stepConfig.getPropertyAsBoolean("plugins.rtm.useLocalDB", true) == true) {
			logger.info("Property 'plugins.rtm.useLocalDB' is set to true, using step collection factory");
			rtmContext = new RtmContext(rtmConfig, context.getCollectionFactory());

		} else {
			logger.info("Property 'plugins.rtm.useLocalDB' is set to false, rtm will use it's own database connection info.");
			rtmContext = new RtmContext(rtmConfig);
		}
		ServletContextHandler servletContextHandler = JettyStarter.getServletContextHandler(rtmContext);
		context.getServiceRegistrationCallback().registerHandler(servletContextHandler);

		Collection<Document> collection = rtmContext.getCollectionFactory().getCollection(MeasurementAccessor.ENTITY_NAME,Document.class);
		collection.createOrUpdateCompoundIndex(MeasurementPlugin.ATTRIBUTE_EXECUTION_ID, MeasurementPlugin.BEGIN);
		collection.createOrUpdateCompoundIndex(MeasurementPlugin.ATTRIBUTE_EXECUTION_ID, MeasurementPlugin.TYPE, MeasurementPlugin.BEGIN);
		collection.createOrUpdateCompoundIndex(MeasurementPlugin.PLAN_ID, MeasurementPlugin.BEGIN);
		collection.createOrUpdateCompoundIndex(MeasurementPlugin.TASK_ID, MeasurementPlugin.BEGIN);
		collection.createOrUpdateIndex(MeasurementPlugin.BEGIN);

		accessor = rtmContext.getMeasurementAccessor();
		context.put(MeasurementAccessor.class, accessor);

		MeasurementPlugin.registerMeasurementHandlers(new RtmHandler(accessor));
	}

	@Override
	public void serverStop(GlobalContext context) {
		if(accessor !=null) {
			accessor.close();
		}
		ExecutorService cleanupExecutorService = rtmContext.getCleanupExecutorService();
		if (cleanupExecutorService != null ) {
			cleanupExecutorService.shutdown();
		}
	}
}
