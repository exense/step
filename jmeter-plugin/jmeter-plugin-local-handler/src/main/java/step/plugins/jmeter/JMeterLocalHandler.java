package step.plugins.jmeter;

import java.io.File;

import javax.json.JsonObject;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.HashTreeTraverser;

import step.functions.handler.AbstractFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;
import step.grid.contextbuilder.ApplicationContextBuilder.ApplicationContext;
import step.grid.filemanager.FileManagerClient.FileVersion;
import step.grid.filemanager.FileManagerClient.FileVersionId;

public class JMeterLocalHandler extends AbstractFunctionHandler {

	String jmeterHome;

	@Override
	public Output<?> handle(Input<?> message) throws Exception {
		ApplicationContext context = getCurrentContext();
		if(context.get("initialized")==null) {
			FileVersionId jmeterLibs = getFileVersionId("$jmeter.libraries", message.getProperties());
			FileVersion jmeterLibFolder = getToken().getServices().getFileManagerClient().requestFileVersion(jmeterLibs.getFileId(), jmeterLibs.getVersion());
			jmeterHome = jmeterLibFolder.getFile().getAbsolutePath();
			updateClasspathSystemProperty();
			
			JMeterUtils.setJMeterHome(jmeterHome);
			JMeterUtils.loadJMeterProperties(jmeterHome+"/bin/jmeter.properties");
			JMeterUtils.initLogging();
			JMeterUtils.initLocale();
			
			context.put("initialized", true);
		}
		
		OutputBuilder out = new OutputBuilder();

		File testPlanFile = retrieveFileVersion("$jmeter.testplan.file", message.getProperties());

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
		// this ugly manipulation of the system property "" is a workaround to
		// the way how the plugins are discovered in jmeter:
		// the method org.apache.jorphan.reflect.ClassFinder.getClasspathMatches
		// relies on the system property "java.class.path"
		// to filter out jar files of the jmeter/lib/ext folder. As this handler
		// relies on URLClassLoader, the extension jars of jmeter
		// are not in the property "java.class.path"
		String cp = System.getProperty("java.class.path");

		File extFolder = new File(jmeterHome+"/lib/ext");
		if (extFolder.exists() && extFolder.isDirectory()) {
			for (File jar : extFolder.listFiles()) {
				cp = cp + ";" + jar.getAbsolutePath();
			}
		}

		System.setProperty("java.class.path", cp);
	}


}
