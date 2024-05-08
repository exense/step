package step.artefacts.handlers.functions.test;

import step.functions.handler.JsonBasedFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;

import javax.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class MyFunctionHandler extends JsonBasedFunctionHandler {

    public static final String HANDLER_ID = "handlerId";
    private static Map<String, Function<Input<JsonObject>, Output<JsonObject>>> handlers = new HashMap<>();

    public static String registerHandler(Function<Input<JsonObject>, Output<JsonObject>> handler) {
        String id = UUID.randomUUID().toString();
        handlers.put(id, handler);
        return id;
    }

    @Override
    public Output<JsonObject> handle(Input<JsonObject> input) throws Exception {
        return handlers.remove(input.getProperties().get(HANDLER_ID)).apply(input);
    }
}
