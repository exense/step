package step.plugins.jmeter;

import step.functions.Input;
import step.functions.Output;
import step.functions.execution.AbstractFunctionHandler;

public class JMeterHandler extends AbstractFunctionHandler {
	
	@Override
	public Output<?> handle(Input<?> input) throws Exception {
		pushRemoteApplicationContext("$jmeter.libraries", input.getProperties());
		
		pushLocalApplicationContext(getClass().getClassLoader(), "jmeter-plugin-local-handler.jar");
		
		return delegate("step.plugins.jmeter.JMeterLocalHandler", input);
	}
}
