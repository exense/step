package step.functions.runner;

import javax.json.Json;
import javax.json.JsonObject;

import step.functions.handler.JsonBasedFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;

public class TestFunctionHandler extends JsonBasedFunctionHandler {

	@Override
	public Output<JsonObject> handle(Input<JsonObject> input) throws Exception {
		Output<JsonObject> output = new Output<>();
		output.setPayload(Json.createObjectBuilder().add("mous", "tache").build());
		return output;
	}

}
