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

import step.grid.agent.AgentTokenServices;
import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.contextbuilder.ApplicationContextBuilder.ApplicationContext;
import step.grid.filemanager.FileManagerClient.FileVersion;
import step.grid.filemanager.FileManagerClient.FileVersionId;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class JMeterLocalHandler extends AbstractMessageHandler {

	String jmeterHome;

	@Override
	public void init(AgentTokenServices tokenServices) {
		super.init(tokenServices);
		// SaveService.loadProperties();
	}

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		ApplicationContext context = token.getServices().getApplicationContextBuilder().getCurrentContext();
		if(context.get("initialized")==null) {
			FileVersionId jmeterLibs = getFileVersionId("$jmeter.libraries", message.getProperties());
			FileVersion jmeterLibFolder = token.getServices().getFileManagerClient().requestFileVersion(jmeterLibs.getFileId(), jmeterLibs.getVersion());
			jmeterHome = jmeterLibFolder.getFile().getAbsolutePath();
			updateClasspathSystemProperty();
			
			JMeterUtils.setJMeterHome(jmeterHome);
			JMeterUtils.loadJMeterProperties(jmeterHome+"/bin/jmeter.properties");
			JMeterUtils.initLogging();
			JMeterUtils.initLocale();
			
			context.put("initialized", true);
		}
		
		OutputMessageBuilder out = new OutputMessageBuilder();

		File testPlanFile = retrieveFileVersion("$jmeter.testplan.file", message.getProperties()).getFile();

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

	private Arguments createArguments(InputMessage message) {
		Arguments arguments = new Arguments();
		JsonObject inputJson = message.getArgument();
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
