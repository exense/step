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
package step.plugins.jmeter;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.HashTreeTraverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.functions.handler.JsonBasedFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;
import step.grid.contextbuilder.ApplicationContext;
import step.grid.filemanager.FileManagerException;
import step.grid.io.AttachmentHelper;

import javax.json.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class JMeterLocalHandler extends JsonBasedFunctionHandler {

	private static final Logger log = LoggerFactory.getLogger(JMeterLocalHandler.class);

	public static final String JMETER_TESTPLAN = "$jmeter.testplan.file";

	public static final String JMETER_LIBRARIES = "$jmeter.libraries";
	public static final String ROOT_LOGGER_JMETER = "RootLoggerJmeter";
	public static final String DEBUG = "debug";

	@Override
	public Output<JsonObject> handle(Input<JsonObject> message) throws Exception {

		ApplicationContext context = getCurrentContext();
		StepAppender appender = null;

		initializeContextIfRequired(message, context);

		Logger rootLogger = (Logger) context.get(ROOT_LOGGER_JMETER);
		if (rootLogger != null) {
			if (rootLogger instanceof ch.qos.logback.classic.Logger) {
				appender = new StepAppender((ch.qos.logback.classic.Logger) rootLogger);
			} else {
				log.warn("rootLogger is not an instance of {}. Actual logger class is {}. The StepAppender cannot be used", ch.qos.logback.classic.Logger.class, rootLogger.getClass());
			}
		}

		OutputBuilder out = new OutputBuilder();

		try (FileVersionCloseable testPlanFileVersion = getTokenReservationSession().putCloseableByHashIfSessionIsAvailable(retrieveFileVersion(JMETER_TESTPLAN, message.getProperties()))) {

			File testPlanFile = testPlanFileVersion.getFile();

			StandardJMeterEngine jmeter = new StandardJMeterEngine();

			HashTree testPlanTree = SaveService.loadTree(testPlanFile);

			Arguments arguments = createArguments(message);
			SampleListenerImpl listener = new SampleListenerImpl(out);

			testPlanTree.traverse(new HashTreeTraverser() {

				@Override
				public void subtractNode() {
				}

				@Override
				public void processPath() {
				}

				@Override
				public void addNode(Object node, HashTree subTree) {
					if (node instanceof TestPlan) {
						testPlanTree.getTree(node).add(listener);
						testPlanTree.getTree(node).add(arguments);
					}
				}
			});

			jmeter.configure(testPlanTree);
			boolean success;
			try {
				jmeter.run();
			} finally {
				success = listener.collect();
			}
			jmeter.reset();

			boolean debug = Boolean.parseBoolean(message.getProperties().getOrDefault(DEBUG, "false"));
			//Appender should always be closed, if errors occurs or debug mode is ON, JMeters logs are attached to the output
			if (appender != null) {
				appender.dispose();
				if (debug || !success) {
					byte[] logData = appender.getData();
					if (logData != null && logData.length > 0) {
						out.addAttachment(AttachmentHelper.generateAttachmentFromByteArray(logData, "log.txt"));
					}
				}
			}

			return out.build();
		}
	}

	private void initializeContextIfRequired(Input<JsonObject> message, ApplicationContext context) throws FileManagerException {
		if(context.get("initialized")==null) {
			Logger rootLogger = null;
			try {
				rootLogger = LoggerFactory.getLogger("ROOT");
			} catch (Exception e) {
				log.error("Unable to obtain root logger, log capturing will not work!", e);
			}

			FileVersionCloseable jmeterLibFileVersion = retrieveFileVersion(JMETER_LIBRARIES, message.getProperties());
			//Make sure the file version is closed whenever the app context is closed
			jmeterLibFileVersion.setInSession(false);
			context.put(String.valueOf(jmeterLibFileVersion.hashCode()), jmeterLibFileVersion);
			File jmeterLibFolder = jmeterLibFileVersion.getFile();

			String jmeterHome = jmeterLibFolder.getAbsolutePath();
			updateClasspathSystemProperty(jmeterHome);

			JMeterUtils.setJMeterHome(jmeterHome);
			JMeterUtils.loadJMeterProperties(jmeterHome+"/bin/jmeter.properties");
			JMeterUtils.initLogging();
			JMeterUtils.initLocale();

			// Add local JMeter properties, if the file is found
			String userProp = JMeterUtils.getPropDefault("user.properties",""); //$NON-NLS-1$
			Properties jmeterProps = JMeterUtils.getJMeterProperties();
			if (!userProp.isEmpty()){ //$NON-NLS-1$
				File file = JMeterUtils.findFile(userProp);
				if (file.canRead()){
					try (FileInputStream fis = new FileInputStream(file)){
						log.info("Loading user properties from: {}", file);
						Properties tmp = new Properties();
						tmp.load(fis);
						jmeterProps.putAll(tmp);
					} catch (IOException e) {
						log.warn("Error loading user property file: {}", userProp, e);
					}
				}
			}

			// Add local system properties, if the file is found
			String sysProp = JMeterUtils.getPropDefault("system.properties",""); //$NON-NLS-1$
			if (!sysProp.isEmpty()){
				File file = JMeterUtils.findFile(sysProp);
				if (file.canRead()) {
					try (FileInputStream fis = new FileInputStream(file)){
						log.info("Loading system properties from: {}", file);
						System.getProperties().load(fis);
					} catch (IOException e) {
						log.warn("Error loading system property file: {}", sysProp, e);
					}
				}
			}

			context.put("initialized", true);
			context.put(ROOT_LOGGER_JMETER, rootLogger);
		}
	}

	private Arguments createArguments(Input<?> input) {
		Map<String, String> argumentMap = new HashMap<>();
		Map<String, String> properties = input.getProperties();
		if (properties != null) {
			properties.forEach((k, v) -> argumentMap.put(k, v));
		}
		// Inputs have precedence over properties
		JsonObject inputJson = (JsonObject) input.getPayload();
		if (inputJson != null) {
			for (String key : inputJson.keySet()) {
				argumentMap.put(key, inputJson.getString(key));
			}
		}
		Arguments arguments = new Arguments();
		argumentMap.forEach((k, v) -> arguments.addArgument(k, v));
		return arguments;
	}

	private void updateClasspathSystemProperty(String jmeterHome) {
		// this ugly manipulation of the system property "java.class.path" is a workaround to
		// the way how the plugins are discovered in jmeter:
		// the method org.apache.jorphan.reflect.ClassFinder.getClasspathMatches
		// relies on the system property "java.class.path"
		// to filter out jar files of the jmeter/lib/ext folder. As this handler
		// relies on URLClassLoader, the extension jars of jmeter
		// are not in the property "java.class.path"
		StringBuilder cp = new StringBuilder(System.getProperty("java.class.path"));

		File extFolder = new File(jmeterHome+"/lib/ext");
		if (extFolder.exists() && extFolder.isDirectory()) {
			for (File jar : Objects.requireNonNull(extFolder.listFiles())) {
				cp.append(File.pathSeparator).append(jar.getAbsolutePath());
			}
		}

		System.setProperty("java.class.path", cp.toString());
	}

}
