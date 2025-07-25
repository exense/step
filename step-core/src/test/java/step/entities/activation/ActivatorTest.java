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

import org.junit.Test;
import step.commons.activation.ActivableObject;
import step.commons.activation.Expression;
import step.expressions.ExpressionHandler;

import javax.script.SimpleBindings;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ActivatorTest {

    private final Activator activator = new Activator(new ExpressionHandler());

    @Test
    public void evaluateActivationExpression() {
        assertTrue(evaluate("1 == 1"));
        assertTrue(evaluate("true"));
        assertFalse(evaluate("false"));
        assertTrue(evaluateWithBindings("var", new SimpleBindings(Map.of("var", true))));
        // For historic reason a wrong expression returns false
        assertFalse(evaluate("\"String\""));
        assertFalse(evaluate("wrong"));
    }

    private boolean evaluate(String script) {
        return evaluateWithBindings(script, new SimpleBindings());
    }

    private boolean evaluateWithBindings(String script, SimpleBindings bindings) {
        return activator.evaluateActivationExpression(bindings, new Expression(script));
    }

    @Test
    public void findBestMatch() {
        TestObject object1 = new TestObject("false");
        TestObject object2 = new TestObject("true");
        TestObject object3 = new TestObject("true", 2);
        TestObject object4 = new TestObject("true", 1);
        TestObject object5 = new TestObject("false", 2);
        TestObject object6 = new TestObject(null, 10);

        TestObject bestMatch = activator.findBestMatch(Map.of(), List.of(object1, object2));
        assertEquals(object2, bestMatch);

        bestMatch = activator.findBestMatch(null, List.of(object3, object4, object5));
        // Object3 has the highest priority
        assertEquals(object3, bestMatch);

        bestMatch = activator.findBestMatch(null, List.of(object5));
        // The object5 doesn't match. We expect null
        assertNull(bestMatch);

        bestMatch = activator.findBestMatch(null, List.of(object6));
        // The object6 has no activation expression. It should match
        assertEquals(object6, bestMatch);

        bestMatch = activator.findBestMatch(null, List.of(object6, object2));
        // Both object match but the object6 without activation expression is considered to have priority 1
        assertEquals(object2, bestMatch);
    }

    @Test
    public void findAllMatches() {
        TestObject object1 = new TestObject("var == '2'");
        TestObject object2 = new TestObject("var == '1'");
        TestObject object3 = new TestObject("var == '2'");
        List<TestObject> allMatches = activator.findAllMatches(Map.of("var", "2"), List.of(object1, object2, object3));
        assertEquals(List.of(object1, object3), allMatches);
        allMatches = activator.findAllMatches(Map.of("var", "3"), List.of(object1, object2, object3));
        assertEquals(List.of(), allMatches);
        allMatches = activator.findAllMatches(null, List.of(object1, object2, object3));
        assertEquals(List.of(), allMatches);
    }

    private static class TestObject implements ActivableObject {

        private final Expression expression;
        private final Integer priority;

        private TestObject(String expression) {
            this(expression, null);
        }

        private TestObject(String expression, Integer priority) {
            this.expression = expression != null ? new Expression(expression) : null;
            this.priority = priority;
        }

        @Override
        public Expression getActivationExpression() {
            return expression;
        }

        @Override
        public Integer getPriority() {
            return priority;
        }
    }
}