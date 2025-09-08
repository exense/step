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
package step.expressions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

public class ExpressionHandlerTest {

    private static final Logger logger = LoggerFactory.getLogger(ExpressionHandlerTest.class);

    @Test
    public void testDefault() {
        Object o;
        try (ExpressionHandler e = new ExpressionHandler()) {
            o = e.evaluateGroovyExpression("1+1", null);
        }
        assertEquals(2,o);
    }

    @Test
    public void testPool() throws Exception {
        // Create factory and spy on it with custom answer
        GroovyPoolFactory realFactory = new GroovyPoolFactory(null); // your scriptBaseClass
        GroovyPoolFactory spyFactory = Mockito.spy(realFactory);
        // Track compilations manually
        AtomicInteger compilationCounter = new AtomicInteger(0);
        Map<String, Integer> expressionCompilationCount = new ConcurrentHashMap<>();
        // Configure spy to track calls
        doAnswer(invocation -> {
            GroovyPoolKey key = invocation.getArgument(0);
            String expression = key.getScript();
            int count = compilationCounter.incrementAndGet();
            expressionCompilationCount.merge(expression, 1, Integer::sum);
            logger.debug("COMPILATION #{} - Expression: '{}'", count,
                    expression.length() > 50 ? expression.substring(0, 50) + "..." : expression);

            // Call the real method
            return invocation.callRealMethod();


        }).when(spyFactory).makeObject(any(GroovyPoolKey.class));
        Object o;
        try (ExpressionHandler e = new ExpressionHandler(null, spyFactory, 2 ,2 ,2, 2, 1)) {
            o = e.evaluateGroovyExpression("1+1", null);
            assertEquals(2, o);
            assertEquals(1, compilationCounter.get());
            assertEquals(1, expressionCompilationCount.get("1+1").intValue());
            o = e.evaluateGroovyExpression("2+2", null);
            assertEquals(4, o);
            assertEquals(2, compilationCounter.get());
            assertEquals(1, expressionCompilationCount.get("2+2").intValue());
            o = e.evaluateGroovyExpression("2+2", null);
            assertEquals(4, o);
            assertEquals(2, compilationCounter.get());
            assertEquals(1, expressionCompilationCount.get("2+2").intValue());
            o = e.evaluateGroovyExpression("3+3", null);
            assertEquals(6, o);
            assertEquals(3, compilationCounter.get());
            assertEquals(1, expressionCompilationCount.get("3+3").intValue());
            o = e.evaluateGroovyExpression("1+1", null);
            assertEquals(2, o);
            assertEquals(4, compilationCounter.get());
            assertEquals(2, expressionCompilationCount.get("1+1").intValue());
        }
    }

    @Test
    public void testBindings() {
        Object o;
        try (ExpressionHandler e = new ExpressionHandler("step.expressions.GroovyFunctions")) {
            Map<String, Object> b = new HashMap<>();
            b.put("test", "value");
            o = e.evaluateGroovyExpression("test", b);
        }
        assertEquals("value", o.toString());
	}

	@Test
	public void testScriptBaseClass() {
        Object o;
        try (ExpressionHandler e = new ExpressionHandler("step.expressions.GroovyFunctions")) {
            o = e.evaluateGroovyExpression("yyyyMMdd", null);
        }
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
        assertEquals(f.format(new Date()), o.toString());
    }

    @Test
    public void testFunction() {
        Object o;
        try (ExpressionHandler e = new ExpressionHandler("step.expressions.GroovyFunctions")) {
            o = e.evaluateGroovyExpression("IsEmpty(\"sts\")", null);
        }
        assertFalse((boolean) o);
    }

    @Test
    public void testScriptBaseClassWithArrays() {
        Object o;
        try (ExpressionHandler e = new ExpressionHandler("step.expressions.GroovyTestFunctions")) {
            o = e.evaluateGroovyExpression("\"${testArrays()[0]}\"", null);
        }
        assertEquals("foo", o.toString());
    }

    @Test
    public void testProtectedBindings() {
        Object o;
        try (ExpressionHandler e = new ExpressionHandler(null)) {
            Map<String, Object> b = new HashMap<>();
            b.put("simpleBinding", "value");
            b.put("protectedBinding", new ProtectedBinding("protectedValue", "protectedBinding"));
            b.put("otherProtectedBinding", new ProtectedBinding("otherProtectedValue", "otherProtectedBinding"));
            o = e.evaluateGroovyExpression("simpleBinding", b, false);
            assertEquals("value", o.toString());
            assertThrows("Error while resolving groovy properties in expression: 'protectedBinding'. The property 'protectedBinding' is protected and can only be used as Keyword's inputs or Keyword's properties.",
                    RuntimeException.class, () -> e.evaluateGroovyExpression("protectedBinding", b, false));
            o = e.evaluateGroovyExpression("simpleBinding", b, true);
            assertEquals("value", o.toString());
            o = e.evaluateGroovyExpression("protectedBinding", b, true);
            assertEquals("***protectedBinding***", o.toString());
            assertTrue(o instanceof ProtectedBinding);
            ProtectedBinding pb = (ProtectedBinding) o;
            assertEquals("protectedValue", pb.value.toString());
            assertEquals("***protectedBinding***", pb.obfuscatedValue);

            assertThrows("Error while resolving groovy properties in expression: 'simpleBinding + \" \" + protectedBinding'. The property 'protectedBinding' is protected and can only be used as Keyword's inputs or Keyword's properties.",
                    RuntimeException.class, () -> e.evaluateGroovyExpression("simpleBinding + \" \"+ protectedBinding", b, false));

            o = e.evaluateGroovyExpression("\"some text: \" + simpleBinding + \" \"+ protectedBinding", b, true);
            assertEquals("some text: value ***protectedBinding***", o.toString());
            assertTrue(o instanceof ProtectedBinding);
            pb = (ProtectedBinding) o;
            assertEquals("some text: value protectedValue", pb.value.toString());
            assertEquals("some text: value ***protectedBinding***", pb.obfuscatedValue);

            o = e.evaluateGroovyExpression("protectedBinding  + \" \" + simpleBinding", b, true);
            assertEquals("***protectedBinding*** value", o.toString());
            assertTrue(o instanceof ProtectedBinding);
            pb = (ProtectedBinding) o;
            assertEquals("protectedValue value", pb.value.toString());
            assertEquals("***protectedBinding*** value", pb.obfuscatedValue);

            o = e.evaluateGroovyExpression("otherProtectedBinding + \" \"+ protectedBinding", b, true);
            Assert.assertEquals("***otherProtectedBinding*** ***protectedBinding***", o.toString());
            Assert.assertEquals("otherProtectedValue protectedValue", ((ProtectedBinding) o).value.toString());
        }

    }

    @Test
    public void testProtectedBindingsWithGString() {
        Object o;
        try (ExpressionHandler e = new ExpressionHandler(null)) {
            Map<String, Object> b = new HashMap<>();
            b.put("simpleBinding", "value");
            b.put("protectedBinding", new ProtectedBinding("protectedValue", "protectedBinding"));
            b.put("otherProtectedBinding", new ProtectedBinding("otherProtectedValue", "otherProtectedBinding"));
            o = e.evaluateGroovyExpression("\"${simpleBinding} ${protectedBinding}\"", b, true);
            assertEquals("value ***protectedBinding***", o.toString());
            assertTrue(o instanceof ProtectedBinding);
            assertEquals("value protectedValue",((ProtectedBinding) o).value.toString());
            assertThrows("Error while resolving groovy properties in expression: '\"${simpleBinding} ${protectedBinding}\"'. The property 'protectedBinding' is protected and can only be used as Keyword's inputs or Keyword's properties.",
                    RuntimeException.class, () -> e.evaluateGroovyExpression("\"${simpleBinding} ${protectedBinding}\"", b, false));

            o = e.evaluateGroovyExpression("\"${simpleBinding} some thing \" + simpleBinding" , b, true);
            assertEquals("value some thing value", o.toString());
            assertTrue(o instanceof String);

            o = e.evaluateGroovyExpression("\"${otherProtectedBinding} some thing \" + protectedBinding" , b, true);
            assertEquals("***otherProtectedBinding*** some thing ***protectedBinding***", o.toString());
            assertTrue(o instanceof ProtectedBinding);
            assertEquals("otherProtectedValue some thing protectedValue",((ProtectedBinding) o).value.toString());

            o = e.evaluateGroovyExpression("\"${simpleBinding} some thing \" + protectedBinding" , b, true);
            assertEquals("value some thing ***protectedBinding***", o.toString());
            assertTrue(o instanceof ProtectedBinding);
            assertEquals("value some thing protectedValue",((ProtectedBinding) o).value.toString());
        }
    }

    @Test
    @Ignore
    public void benchmarkPoolLegacyPoolExhausted() throws Exception {
        int threadsPerExpression0 = 8;
        int threadsPerExpression1 = 20;
        int threadsPerExpression2 = 50;
        int threadsPerExpression3 = 70;
        int threadsPerExpression4 = 100;
        int poolSize = 1000;
        int maxPoolPerKey = 8;
        int iterationsPerThread = 50;

        benchmarkGroovyPoolConfig( threadsPerExpression0, iterationsPerThread, poolSize, maxPoolPerKey, -1);
        benchmarkGroovyPoolConfig( threadsPerExpression0, iterationsPerThread, poolSize, maxPoolPerKey, -1);
        benchmarkGroovyPoolConfig(threadsPerExpression1, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig(threadsPerExpression2, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig(threadsPerExpression3, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig(threadsPerExpression4, iterationsPerThread, poolSize, maxPoolPerKey, -1);;

        maxPoolPerKey=50;
        benchmarkGroovyPoolConfig( threadsPerExpression0, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig( threadsPerExpression0, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig(threadsPerExpression1, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig(threadsPerExpression2, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig(threadsPerExpression3, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig(threadsPerExpression4, iterationsPerThread, poolSize, maxPoolPerKey, -1);;

        maxPoolPerKey=100;
        benchmarkGroovyPoolConfig( threadsPerExpression0, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig( threadsPerExpression0, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig(threadsPerExpression1, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig(threadsPerExpression2, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig(threadsPerExpression3, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig(threadsPerExpression4, iterationsPerThread, poolSize, maxPoolPerKey, -1);;

        maxPoolPerKey=200;
        benchmarkGroovyPoolConfig( threadsPerExpression0, iterationsPerThread, poolSize, maxPoolPerKey, -1);
        benchmarkGroovyPoolConfig( threadsPerExpression0, iterationsPerThread, poolSize, maxPoolPerKey, -1);
        benchmarkGroovyPoolConfig(threadsPerExpression1, iterationsPerThread, poolSize, maxPoolPerKey, -1);
        benchmarkGroovyPoolConfig(threadsPerExpression2, iterationsPerThread, poolSize, maxPoolPerKey, -1);
        benchmarkGroovyPoolConfig(threadsPerExpression3, iterationsPerThread, poolSize, maxPoolPerKey, -1);
        benchmarkGroovyPoolConfig(threadsPerExpression4, iterationsPerThread, poolSize, maxPoolPerKey, -1);


        maxPoolPerKey=1000;
        benchmarkGroovyPoolConfig( threadsPerExpression0, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig( threadsPerExpression0, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig(threadsPerExpression1, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig(threadsPerExpression2, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig(threadsPerExpression3, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
        benchmarkGroovyPoolConfig(threadsPerExpression4, iterationsPerThread, poolSize, maxPoolPerKey, -1);;
    }



    private static void benchmarkGroovyPoolConfig(int threadsPerExpression, int iterationsPerThread, int poolSize, int maxPoolPerKey, int maxIdlePerKey) throws Exception {
        logger.info(">>>> Benchmark with poolSize {}, threadsPerExpression {}, iterationsPerThread {}, maxPoolPerKey {}, maxIdlePerKey {}"
                , poolSize, threadsPerExpression, iterationsPerThread, maxPoolPerKey, maxIdlePerKey);

        final String EXPRESSION_1 = "Thread.sleep(20); return 20;";
        final String EXPRESSION_2 = "Thread.sleep(1); return 1;";
        final String EXPRESSION_1_BIS = "Thread.sleep(21); return 21;;";
        final String EXPRESSION_2_BIS = "Thread.sleep(2); return 2;";

        // Track compilations manually
        AtomicInteger compilationCounter = new AtomicInteger(0);
        Map<String, Integer> expressionCompilationCount = new ConcurrentHashMap<>();
        Map<String, Long> expressionCompilationDuration = new ConcurrentHashMap<>();

        // Create factory and spy on it with custom answer
        GroovyPoolFactory realFactory = new GroovyPoolFactory(null); // your scriptBaseClass
        GroovyPoolFactory spyFactory = Mockito.spy(realFactory);

        // Configure spy to track calls
        doAnswer(invocation -> {
            GroovyPoolKey key = invocation.getArgument(0);
            String expression = key.getScript();

            int count = compilationCounter.incrementAndGet();
            expressionCompilationCount.merge(expression, 1, Integer::sum);

            logger.debug("COMPILATION #{} - Expression: '{}'", count,
                    expression.length() > 50 ? expression.substring(0, 50) + "..." : expression);

            // Call the real method
            long startCompilation = System.nanoTime();
            Object o = invocation.callRealMethod();
            expressionCompilationDuration.merge(expression, System.nanoTime() - startCompilation, Long::sum);
            return o;

        }).when(spyFactory).makeObject(any(GroovyPoolKey.class));

        try (ExpressionHandler handler = new ExpressionHandler( null, spyFactory,null, poolSize, maxPoolPerKey, maxIdlePerKey, 1)) {

            logger.info("=== PHASE 1: Expression 1 dominates the pool ===");

            // Phase 1: Fill pool with Expression 1
            ExecutorService executor1 = Executors.newFixedThreadPool(threadsPerExpression);
            CountDownLatch phase1Latch = new CountDownLatch(threadsPerExpression);
            AtomicLong expr1TotalTime = new AtomicLong(0);

            long phase1StartTime = System.nanoTime();
            executeExpressionInParaallel(threadsPerExpression, iterationsPerThread, executor1, handler, EXPRESSION_1, expr1TotalTime, phase1Latch);

            // Wait for Phase 1 completion
            assertTrue("Phase 1 should complete within 300 seconds", phase1Latch.await(300, TimeUnit.SECONDS));
            executor1.shutdown();

            double phase1DurationMs = (System.nanoTime() - phase1StartTime) / 1_000_000.0;

            String compilationStatsPhase1 = getCompilationDetails(expressionCompilationCount, expressionCompilationDuration);


            // Small pause to ensure all objects are returned to pool
            Thread.sleep(100);

            //reset counts
            expressionCompilationCount.clear();
            expressionCompilationDuration.clear();

            logger.info("=== PHASE 2: Expression 2 competes for pool space ===");

            // Phase 2: Introduce Expression 2 while continuing Expression 1
            ExecutorService executor2 = Executors.newFixedThreadPool(threadsPerExpression * 4);
            CountDownLatch phase2Latch = new CountDownLatch(threadsPerExpression * 4);

            AtomicLong expr1Phase2TotalTime = new AtomicLong(0);
            AtomicLong expr1BisTotalTime = new AtomicLong(0);
            AtomicLong expr2TotalTime = new AtomicLong(0);
            AtomicLong expr2BisTotalTime = new AtomicLong(0);

            long phase2StartTime = System.nanoTime();

            executeExpressionInParaallel(threadsPerExpression, iterationsPerThread, executor2, handler, EXPRESSION_1, expr1Phase2TotalTime, phase2Latch);
            executeExpressionInParaallel(threadsPerExpression, iterationsPerThread, executor2, handler, EXPRESSION_1_BIS, expr1BisTotalTime, phase2Latch);

            executeExpressionInParaallel(threadsPerExpression, iterationsPerThread, executor2, handler, EXPRESSION_2, expr2TotalTime, phase2Latch);
            executeExpressionInParaallel(threadsPerExpression, iterationsPerThread, executor2, handler, EXPRESSION_2_BIS, expr2BisTotalTime, phase2Latch);

            // Wait for Phase 2 completion
            assertTrue("Phase 2 should complete within 900 seconds", phase2Latch.await(900, TimeUnit.SECONDS));
            executor2.shutdown();

            double phase2DurationMs = (System.nanoTime() - phase2StartTime) / 1_000_000.0;
            logger.info("Phase 2 completed - Duration {}ms", phase2DurationMs);

            String compilationStatsPhase2 = getCompilationDetails(expressionCompilationCount, expressionCompilationDuration);
            // Results Analysis

            // Performance comparison
            double expr1Phase1Avg = expr1TotalTime.get() / (double) threadsPerExpression / iterationsPerThread / 1_000_000.0;
            double expr1Phase2Avg = expr1Phase2TotalTime.get() / (double) threadsPerExpression / iterationsPerThread / 1_000_000.0;
            double expr2Avg = expr2TotalTime.get() / (double) threadsPerExpression / iterationsPerThread / 1_000_000.0;

            logger.info("<<<< Summary results Benchmark with poolSize {}, threadsPerExpression {}, iterationsPerThread {}, maxPoolPerKey {}, maxIdlePerKey {}}"
                    , poolSize, threadsPerExpression, iterationsPerThread, maxPoolPerKey, maxIdlePerKey);
            logger.info("Performance compilations phase 1: {}", compilationStatsPhase1);
            logger.info("Performance compilations phase 2: {}", compilationStatsPhase2);
            logger.info("Performance - Total compilations {}, Phase1 {}ms, Phase2 {}ms, Expr1 Phase1: {}ms, Expr1 Phase2: {}ms, Expr2: {}ms",
                    compilationCounter.get(), phase1DurationMs, phase2DurationMs, expr1Phase1Avg, expr1Phase2Avg, expr2Avg);
        }
    }

    private static String getCompilationDetails(Map<String, Integer> expressionCompilationCount, Map<String, Long> expressionCompilationDuration) {
        StringBuilder stringBuffer = new StringBuilder("Compilations by expression:");
        expressionCompilationCount.forEach((expr, count) ->
                stringBuffer.append("'").append(expr.substring(0, Math.min(30, expr.length()))).append("' -> ").append(count).append(" compilations, durations ")
                        .append((double) expressionCompilationDuration.get(expr) / count /  1_000_000.0).append("ms,"));
        return stringBuffer.toString();
    }

    private static void executeExpressionInParaallel(int threadsPerExpression, int iterationsPerThread, ExecutorService executor1, ExpressionHandler handler, String EXPRESSION_1, AtomicLong expr1TotalTime, CountDownLatch phase1Latch) {
        for (int i = 0; i < threadsPerExpression; i++) {
            final int input = i;
            executor1.submit(() -> {
                try {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        long startTime = System.nanoTime();
                        Object result = handler.evaluateGroovyExpression(EXPRESSION_1, Map.of("input", input));
                        long duration = System.nanoTime() - startTime;
                        expr1TotalTime.addAndGet(duration);
                        assertNotNull(result);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Expression {} result for input {}: {}", EXPRESSION_1, input, result);
                        }
                    }

                } catch(Exception e){
                    logger.error("Error in expression {} evaluation", EXPRESSION_1, e);
                    fail("Expression " + EXPRESSION_1 + " failed: " + e.getMessage());
                } finally{
                    phase1Latch.countDown();
                }
            });
        }
    }
}
