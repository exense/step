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

package step.commons.activation;

import org.junit.Test;

import javax.script.SimpleBindings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
public class ActivatorTest {

    @Test
    public void evaluateActivationExpression() {
        assertTrue(evaluate("1 == 1"));
        assertTrue(evaluate("true"));
        assertFalse(evaluate("false"));
        assertTrue(evaluateWithBindings("myVar", new SimpleBindings(Map.of("myVar", true))));
        // For historic reason a wrong expression returns false
        assertFalse(evaluate("\"String\""));
        assertFalse(evaluate("wrong"));
    }

    @Test
    public void testParallel() throws ExecutionException, InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        ArrayList<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Future<?> future = executor.submit(() -> {
                for (int j = 0; j < 1000; j++) {
                    assertTrue(evaluateWithBindings("myVar", new SimpleBindings(Map.of("myVar", true))));
                    assertFalse(evaluateWithBindings("myVar", new SimpleBindings(Map.of("myVar", false))));
                    assertTrue(evaluateWithBindings("myVar == " + j, new SimpleBindings(Map.of("myVar", j))));
                }
            });
            futures.add(future);
        }
        for (Future<?> future : futures) {
            future.get();
        }
    }

    private boolean evaluate(String script) {
        return evaluateWithBindings(script, new SimpleBindings());
    }

    private boolean evaluateWithBindings(String script, SimpleBindings bindings) {
        return Activator.evaluateActivationExpression(bindings, new Expression(script), "nashorn");
    }

    @Test
    public void findBestMatch() {
        TestObject object1 = new TestObject("false");
        TestObject object2 = new TestObject("true");
        TestObject object3 = new TestObject("true", 2);
        TestObject object4 = new TestObject("true", 1);
        TestObject object5 = new TestObject("false", 2);
        TestObject object6 = new TestObject(null, 10);

        TestObject bestMatch = Activator.findBestMatch(Map.of(), List.of(object1, object2), "nashorn");
        assertEquals(object2, bestMatch);

        bestMatch = Activator.findBestMatch(null, List.of(object3, object4, object5), "nashorn");
        // Object3 has the highest priority
        assertEquals(object3, bestMatch);

        bestMatch = Activator.findBestMatch(null, List.of(object5), "nashorn");
        // The object5 doesn't match. We expect null
        assertNull(bestMatch);

        bestMatch = Activator.findBestMatch(null, List.of(object6), "nashorn");
        // The object6 has no activation expression. It should match
        assertEquals(object6, bestMatch);

        bestMatch = Activator.findBestMatch(null, List.of(object6, object2), "nashorn");
        // Both object match but the object6 without activation expression is considered to have priority 1
        assertEquals(object2, bestMatch);
    }

    @Test
    public void findAllMatches() {
        TestObject object1 = new TestObject("myVar == '2'");
        TestObject object2 = new TestObject("myVar == '1'");
        TestObject object3 = new TestObject("myVar == '2'");
        List<TestObject> allMatches = Activator.findAllMatches(Map.of("myVar", "2"), List.of(object1, object2, object3), "nashorn");
        assertEquals(List.of(object1, object3), allMatches);
        allMatches = Activator.findAllMatches(Map.of("myVar", "3"), List.of(object1, object2, object3), "nashorn");
        assertEquals(List.of(), allMatches);
        allMatches = Activator.findAllMatches(null, List.of(object1, object2, object3), "nashorn");
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