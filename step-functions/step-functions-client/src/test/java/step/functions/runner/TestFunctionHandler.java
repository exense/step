package step.functions.runner;

import javax.json.Json;
import javax.json.JsonObject;

import step.functions.handler.AbstractFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;

public class TestFunctionHandler extends AbstractFunctionHandler {

	@Override
	public Output<?> handle(Input<?> input) throws Exception {
		Output<JsonObject> output = new Output<>();
		output.setPayload(Json.createObjectBuilder().add("mous", "tache").build());
		return output;
	}

}
