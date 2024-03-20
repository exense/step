package step.artefacts.handlers.functions;

import step.functions.execution.FunctionExecutionService;
import step.grid.tokenpool.Interest;

import java.util.Map;

public interface TokenNumberCalculationContext {

    void setParent(TokenNumberCalculationContext parentContext);
    String requireToken(Map<String, Interest> criteria, int count);

    void requireToken(String pool, int count);

    void releaseRequiredToken(String pool, int count);

    FunctionExecutionService getFunctionExecutionServiceForTokenRequirementCalculation();
}
