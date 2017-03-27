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
import step.grid.agent.handler.AgentContextAware;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class JMeterLocalHandler implements MessageHandler, AgentContextAware {

	public static final String JMETER_TESTPLAN_FILE_VERSION = "$jmeter.testplan.file.version";
	public static final String JMETER_TESTPLAN_FILE_ID = "$jmeter.testplan.file.id";
	
	String jmeterHome;
	
	public JMeterLocalHandler() {
		super();
	}
	

	@Override
	public void init(AgentTokenServices tokenServices) {
		updateClasspathSystemProperty();

		jmeterHome = tokenServices.getAgentProperties().get("plugins.jmeter.home");
		
		JMeterUtils.setJMeterHome(jmeterHome);
		JMeterUtils.loadJMeterProperties(jmeterHome+"/bin/jmeter.properties");
		JMeterUtils.initLogging();
		JMeterUtils.initLocale();
		// SaveService.loadProperties();
	}

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		OutputMessageBuilder out = new OutputMessageBuilder();

		File testPlanFile = retrieveTestPlanFile(token, message);

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

	private File retrieveTestPlanFile(AgentTokenWrapper token, InputMessage message) {
		String testplanFileId = message.getProperties().get(JMETER_TESTPLAN_FILE_ID);
		String testplanFileVersion = message.getProperties().get(JMETER_TESTPLAN_FILE_VERSION);

		File testPlanFile = token.getServices().getFileManagerClient().requestFile(testplanFileId,
				Long.parseLong(testplanFileVersion));
		return testPlanFile;
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
