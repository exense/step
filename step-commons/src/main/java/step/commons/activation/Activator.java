/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.commons.activation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Activator {

    public static final String DEFAULT_SCRIPT_ENGINE = "groovy";

    public static final Logger logger = LoggerFactory.getLogger(Activator.class);

    private static void compileExpression(Expression expression, String defaultScriptEngine) throws ScriptException {
        if (expression != null && expression.compiledScript == null) {
            String scriptEngine = expression.scriptEngine != null ? expression.scriptEngine : defaultScriptEngine;

            if (expression.script != null && expression.script.trim().length() > 0) {
                ScriptEngineManager manager = new ScriptEngineManager();
                ScriptEngine engine = manager.getEngineByName(scriptEngine);

                CompiledScript script = ((Compilable) engine).compile(expression.script);
                expression.compiledScript = script;
            } else {
                expression.compiledScript = null;
            }
        }
    }

    private static GroovyExpressionHandler groovyExpressionHandler;

    public static void setGroovyExpressionHandler(GroovyExpressionHandler groovyExpressionHandler) {
        Activator.groovyExpressionHandler = groovyExpressionHandler;
    }

    public static Boolean evaluateActivationExpression(Bindings bindings, Expression activationExpression, String defaultScriptEngine) {
        Boolean expressionResult;
        if (activationExpression != null) {
            // This block redirects Groovy evaluations to the groovyExpressionHandler because of a memory leak when using script.eval() with Groovy;
            // the alternative implementation also provides much better performance by caching expressions.
            // Other languages are unaffected, and if no handler is present it also uses the old path, but logs warnings on each evaluation.
            if (activationExpression.script != null && !activationExpression.script.trim().isBlank()) {
                String scriptEngine = activationExpression.scriptEngine != null ? activationExpression.scriptEngine : defaultScriptEngine;
                if ("groovy".equals(scriptEngine)) {
                    if (groovyExpressionHandler != null) {
                        try {
                            Object result = groovyExpressionHandler.evaluateGroovyExpression(activationExpression.getScript(), bindings);
                            if (result instanceof Boolean bool) {
                                return bool;
                            } else {
                                logger.warn("Groovy expression did not return a boolean result, interpreting as 'false': {} == {} ", activationExpression.script, result);
                                return false;
                            }
                        } catch (Exception e) {
                            // backward-compatible behavior
                            logger.warn("Evaluation of Groovy expression threw an exception, returning 'false': {}", activationExpression.script, e);
                            return false;
                        }
                    } else {
                        logger.warn("No groovyExpressionHandler was found; using legacy code path that may leak memory over time; expression: {}", activationExpression.script);
                    }
                }
            }
            try {
                compileExpression(activationExpression, defaultScriptEngine);
            } catch (ScriptException e1) {
                logger.error("Error while evaluating expression " + activationExpression, e1);
            }
            CompiledScript script = activationExpression.compiledScript;
            if (script != null) {
                try {
                    Object evaluationResult = script.eval(bindings);
                    if (evaluationResult instanceof Boolean) {
                        expressionResult = (Boolean) evaluationResult;
                    } else {
                        expressionResult = false;
                    }
                } catch (ScriptException e) {
                    expressionResult = false;
                }
            } else {
                expressionResult = true;
            }
        } else {
            expressionResult = true;
        }
        return expressionResult;
    }

    public static <T extends ActivableObject> T findBestMatch(Map<String, Object> bindings, List<T> objects, String defaultScriptEngine) {
        return findBestMatch(bindings != null ? new SimpleBindings(bindings) : null, objects, defaultScriptEngine);
    }

    private static <T extends ActivableObject> T findBestMatch(Bindings bindings, List<T> objects, String defaultScriptEngine) {

        List<T> matchingObjects = new ArrayList<>(objects);
        matchingObjects.sort(new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return -Integer.compare(getPriority(o1), getPriority(o2));
            }

            private int getPriority(T o1) {
                if (o1.getPriority() != null) {
                    return o1.getPriority();
                } else {
                    return o1.getActivationExpression() != null ? 1 : 0;
                }
            }
        });

        for (T object : matchingObjects) {
            if (evaluateActivationExpression(bindings, object.getActivationExpression(), defaultScriptEngine)) {
                return object;
            }
        }
        return null;
    }

    public static <T extends ActivableObject> List<T> findAllMatches(Map<String, Object> bindings, List<T> objects, String defaultScriptEngine) {
        List<T> result = new ArrayList<>();
        for (T object : objects) {
            Boolean expressionResult = evaluateActivationExpression(bindings != null ? new SimpleBindings(bindings) : null, object.getActivationExpression(), defaultScriptEngine);

            if (expressionResult) {
                result.add(object);
            }
        }
        return result;
    }

    public static GroovyExpressionHandler getGroovyExpressionHandler() {
        return groovyExpressionHandler;
    }
}

