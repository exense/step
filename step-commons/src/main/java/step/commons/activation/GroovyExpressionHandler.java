package step.commons.activation;

import java.util.Map;

/**
 * This interface allows to redirect (only) Groovy expression evaluation to a different
 * implementation (defined outside of this module).
 */
public interface GroovyExpressionHandler extends AutoCloseable {
    Object evaluateGroovyExpression(String expression, Map<String, Object> bindings) throws Exception;
}
