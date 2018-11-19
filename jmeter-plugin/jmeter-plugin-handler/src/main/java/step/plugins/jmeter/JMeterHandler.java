package step.plugins.jmeter;

import javax.json.JsonObject;

import step.functions.handler.JsonBasedFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;

public class JMeterHandler extends JsonBasedFunctionHandler {
	
	@Override
	public Output<JsonObject> handle(Input<JsonObject> input) throws Exception {
		pushRemoteApplicationContext("$jmeter.libraries", input.getProperties());
		
		pushLocalApplicationContext(getClass().getClassLoader(), "jmeter-plugin-local-handler.jar");
		
		return delegate("step.plugins.jmeter.JMeterLocalHandler", input);
	}
}
