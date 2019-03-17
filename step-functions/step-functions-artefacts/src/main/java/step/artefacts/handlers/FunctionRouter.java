package step.artefacts.handlers;

import java.util.Map;

import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.functions.Function;
import step.functions.execution.FunctionExecutionServiceException;
import step.grid.TokenWrapper;

public interface FunctionRouter {

	TokenWrapper selectToken(CallFunction callFunction, Function function, FunctionGroupContext functionGroupContext,
			Map<String, Object> bindings) throws FunctionExecutionServiceException;

}