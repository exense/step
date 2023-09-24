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
import step.grid.contextbuilder.ApplicationContextBuilder.ApplicationContext;
import step.grid.io.AttachmentHelper;

import javax.json.JsonObject;
import java.io.File;
import java.util.Objects;

public class JMeterLocalHandler extends JsonBasedFunctionHandler {

	public static final String JMETER_TESTPLAN = "$jmeter.testplan.file";

	public static final String JMETER_LIBRARIES = "$jmeter.libraries";

	protected String jmeterHome;
	protected Logger rootLogger;

	@Override
	public Output<JsonObject> handle(Input<JsonObject> message) throws Exception {

		ApplicationContext context = getCurrentContext();
		StepAppender appender = null;

		if(context.get("initialized")==null) {
			try {
				rootLogger = (Logger) LoggerFactory.getLogger("ROOT");
			} catch (Exception e) {
				System.err.println("Unable to obtain root logger, log capturing will not work!");
				e.printStackTrace();
			}

			File jmeterLibFolder = retrieveFileVersion(JMETER_LIBRARIES, message.getProperties());

			jmeterHome = jmeterLibFolder.getAbsolutePath();
			updateClasspathSystemProperty();

			JMeterUtils.setJMeterHome(jmeterHome);
			JMeterUtils.loadJMeterProperties(jmeterHome+"/bin/jmeter.properties");
			JMeterUtils.initLogging();
			JMeterUtils.initLocale();

			context.put("initialized", true);
		}

		// TODO: slf4j or logback?
		if (rootLogger != null && rootLogger instanceof ch.qos.logback.classic.Logger) {
			appender = new StepAppender((ch.qos.logback.classic.Logger) rootLogger);
		}

		OutputBuilder out = new OutputBuilder();

		File testPlanFile = retrieveFileVersion(JMETER_TESTPLAN, message.getProperties());

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
		try {
			jmeter.run();
		} finally {
			listener.collect();
		}

		if (appender != null) {
			appender.dispose();
			byte[] logData = appender.getData();
			if (logData != null && logData.length > 0) {
				out.addAttachment(AttachmentHelper.generateAttachmentFromByteArray(logData, "log.txt"));
			}
		}


		return out.build();

	}

	private Arguments createArguments(Input<?> input) {
		Arguments arguments = new Arguments();
		JsonObject inputJson = (JsonObject) input.getPayload();
		if (inputJson != null) {
			for (String key : inputJson.keySet()) {
				arguments.addArgument(key, inputJson.getString(key));
			}
		}
		return arguments;
	}

	private void updateClasspathSystemProperty() {
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
