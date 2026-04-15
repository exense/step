package step.artefacts.handlers.functions.test;

import jakarta.json.JsonObject;
import step.functions.Function;
import step.functions.io.Input;
import step.functions.io.Output;

public class MyFunction extends Function {

    private final java.util.function.Function<Input<JsonObject>, Output<JsonObject>> handler;

    public MyFunction(java.util.function.Function<Input<JsonObject>, Output<JsonObject>> handler) {
        this.handler = handler;
    }

    public java.util.function.Function<Input<JsonObject>, Output<JsonObject>> getHandler() {
        return handler;
    }

    @Override
    public boolean requiresLocalExecution() {
        return false;
    }
}
