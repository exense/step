package step.commons.activation;

import java.util.Map;

/**
 * This interface allows to redirect (only) Groovy expression evaluation to a different
 * implementation (defined outside of this module). This is necessary because the default
 * strategy using Script.eval() can cause memory leaks for Groovy.
 */
public interface GroovyExpressionHandler {
    Object evaluateGroovyExpression(String expression, Map<String, Object> bindings) throws Exception;
}
