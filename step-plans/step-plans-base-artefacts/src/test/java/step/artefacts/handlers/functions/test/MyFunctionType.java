package step.artefacts.handlers.functions.test;

import step.core.AbstractStepContext;
import step.functions.type.AbstractFunctionType;

import java.util.Map;

public class MyFunctionType extends AbstractFunctionType<MyFunction> {

    @Override
    public String getHandlerChain(MyFunction function) {
        return MyFunctionHandler.class.getName();
    }

    @Override
    public HandlerProperties getHandlerProperties(MyFunction function, AbstractStepContext executionContext) {
        String handlerId = MyFunctionHandler.registerHandler(function.getHandler());
        return new HandlerProperties(Map.of(MyFunctionHandler.HANDLER_ID, handlerId));
    }

    @Override
    public MyFunction newFunction() {
        return new MyFunction(null);
    }
}
