package step.plugins.jmeter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import step.functions.FunctionClient;
import step.functions.type.AbstractFunctionType;
import step.plugins.adaptergrid.GridPlugin;

public class JMeterFunctionType extends AbstractFunctionType<JMeterFunction> {

	@Override
	public String getHandlerChain(JMeterFunction function) {
		return "class:step.plugins.jmeter.JMeterHandler";
	}

	@Override
	public Map<String, String> getHandlerProperties(JMeterFunction function) {
		Map<String, String> props = new HashMap<>();
		String testplanFilepath = function.getJmeterTestplan().get();
		File testPlanFile = new File(testplanFilepath);
		
		FunctionClient functionClient = (FunctionClient) context.get(GridPlugin.FUNCTIONCLIENT_KEY);
		String fileHandle = functionClient.registerAgentFile(testPlanFile);
		
		props.put(JMeterLocalHandler.JMETER_TESTPLAN_FILE_ID, fileHandle);
		props.put(JMeterLocalHandler.JMETER_TESTPLAN_FILE_VERSION, Long.toString(testPlanFile.lastModified()));
		
		return props;
	}

	@Override
	public JMeterFunction newFunction() {
		return new JMeterFunction();
	}

}
