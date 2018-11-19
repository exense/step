package step.functions.handler;

import javax.json.JsonObject;

public abstract class JsonBasedFunctionHandler extends AbstractFunctionHandler<JsonObject, JsonObject> {

	@Override
	public Class<JsonObject> getInputPayloadClass() {
		return JsonObject.class;
	}

	@Override
	public Class<JsonObject> getOutputPayloadClass() {
		return JsonObject.class;
	}

}
