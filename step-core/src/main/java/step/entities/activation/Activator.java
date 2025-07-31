/*
 * Copyright (C) 2025, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */
package step.entities.activation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.commons.activation.ActivableObject;
import step.commons.activation.Expression;
import step.expressions.ExpressionHandler;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Activator {

    public static final Logger logger = LoggerFactory.getLogger(Activator.class);
    private final ExpressionHandler expressionHandler;

    public Activator(ExpressionHandler expressionHandler) {
        this.expressionHandler = expressionHandler;
    }

    public boolean evaluateActivationExpression(Map<String, Object> bindings, Expression activationExpression) {
        boolean result;
        if (activationExpression != null) {
            try {
                Bindings newBindings = bindings != null ? new SimpleBindings(bindings) : new SimpleBindings();
                Object evaluationResult = expressionHandler.evaluateGroovyExpression(activationExpression.getScript(), newBindings);
                if (evaluationResult instanceof Boolean) {
                    result = (Boolean) evaluationResult;
                } else {
                    // For historical reasons we're returning false when the result of an expression is not a boolean
                    // Changing this could cause unexpected exceptions where ever activation expression are executed
                    logger.warn("The evaluation of the activation expression {} did not return a boolean. Returning false by default", activationExpression.getScript());
                    result = false;
                }
            } catch (Exception e) {
                // For historical reasons we're returning false when the execution of an expression throws an error
                // Changing this could cause unexpected exceptions where ever activation expression are executed
                logger.warn("The evaluation of the activation expression {} failed. Returning false by default", activationExpression.getScript(), e);
                result = false;
            }
        } else {
            // Objects without activation expression are always matched
            result = true;
        }
        return result;
    }

    public <T extends ActivableObject> T findBestMatch(Map<String, Object> bindings, List<T> objects) {
        List<T> matchingObjects = new ArrayList<>(objects);
        matchingObjects.sort(new Comparator<>() {
            @Override
            public int compare(T o1, T o2) {
                return -Integer.compare(getPriority(o1), getPriority(o2));
            }

            private int getPriority(T o1) {
                return o1.getActivationExpression() == null ? 0 : (o1.getPriority() == null ? 1 : o1.getPriority());
            }
        });

        for (T object : matchingObjects) {
            if (evaluateActivationExpression(bindings, object.getActivationExpression())) {
                return object;
            }
        }
        return null;
    }

    public <T extends ActivableObject> List<T> findAllMatches(Map<String, Object> bindings, List<T> objects) {
        return objects.stream().filter(object -> evaluateActivationExpression(bindings, object.getActivationExpression())).collect(Collectors.toList());
    }

}

