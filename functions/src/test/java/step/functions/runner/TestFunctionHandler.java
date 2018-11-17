package step.functions.runner;

import javax.json.Json;
import javax.json.JsonObject;

import step.functions.Input;
import step.functions.Output;
import step.functions.execution.AbstractFunctionHandler;

public class TestFunctionHandler extends AbstractFunctionHandler {

	@Override
	public Output<?> handle(Input<?> input) throws Exception {
		Output<JsonObject> output = new Output<>();
		output.setPayload(Json.createObjectBuilder().add("mous", "tache").build());
		return output;
	}

}
