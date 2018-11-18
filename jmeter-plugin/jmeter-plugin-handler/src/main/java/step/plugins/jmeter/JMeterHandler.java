package step.plugins.jmeter;

import step.functions.handler.AbstractFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;

public class JMeterHandler extends AbstractFunctionHandler {
	
	@Override
	public Output<?> handle(Input<?> input) throws Exception {
		pushRemoteApplicationContext("$jmeter.libraries", input.getProperties());
		
		pushLocalApplicationContext(getClass().getClassLoader(), "jmeter-plugin-local-handler.jar");
		
		return delegate("step.plugins.jmeter.JMeterLocalHandler", input);
	}
}
