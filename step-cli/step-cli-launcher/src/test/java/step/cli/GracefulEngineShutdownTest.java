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
package step.cli;

import org.junit.Test;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GracefulEngineShutdownTest {

    private final ExecutionEngine engine = mock(ExecutionEngine.class);
    private final List<ExecutionContext> aborted = new ArrayList<>();

    private GracefulEngineShutdown shutdown(long timeoutMs) {
        return new GracefulEngineShutdown(engine, timeoutMs, aborted::add);
    }

    /**
     * The normal path. The CLI runs its plans synchronously, so by the time the try-with-resources closes
     * the engine there is nothing left running and this must be the plain close it wraps.
     */
    @Test
    public void closesTheEngineDirectlyWhenNothingIsRunning() {
        when(engine.getCurrentExecutions()).thenReturn(List.of());

        shutdown(GracefulEngineShutdown.DEFAULT_TIMEOUT_MS).close();

        assertEquals(List.of(), aborted);
        verify(engine).close();
    }

    /**
     * The interrupted path, and the whole point of this class: closing an engine under a live execution
     * pulls files out from under a keyword that still has them open.
     */
    @Test
    public void abortsEveryRunningExecutionBeforeClosingTheEngine() {
        ExecutionContext first = mock(ExecutionContext.class);
        ExecutionContext second = mock(ExecutionContext.class);
        when(engine.getCurrentExecutions()).thenReturn(List.of(first, second), List.of());

        shutdown(GracefulEngineShutdown.DEFAULT_TIMEOUT_MS).close();

        assertEquals(List.of(first, second), aborted);
        verify(engine).close();
    }

    /**
     * Aborting is asynchronous - it moves the execution to {@code ABORTING} and returns - so the engine may
     * only be closed once the executions have actually ended.
     */
    @Test
    public void waitsForTheExecutionsToEndBeforeClosingTheEngine() {
        ExecutionContext running = mock(ExecutionContext.class);
        AtomicBoolean engineClosed = new AtomicBoolean();
        List<Boolean> stillRunningWhenPolled = new ArrayList<>();
        when(engine.getCurrentExecutions()).thenAnswer(invocation -> {
            stillRunningWhenPolled.add(!engineClosed.get());
            // Ends on the third look: once to find it, once still running, once gone
            return stillRunningWhenPolled.size() < 3 ? List.of(running) : List.of();
        });
        doOnClose(engineClosed);

        shutdown(GracefulEngineShutdown.DEFAULT_TIMEOUT_MS).close();

        assertEquals(List.of(running), aborted);
        assertTrue("the engine must not have been closed while an execution was still running",
            stillRunningWhenPolled.stream().allMatch(Boolean::booleanValue));
        verify(engine).close();
    }

    /**
     * A keyword that ignores its abort must not keep the process alive: the wait is bounded and the engine
     * is closed anyway, at the risk of the cleanup failing on a file still held.
     */
    @Test
    public void closesTheEngineAnywayWhenTheExecutionsOutlastTheTimeout() {
        when(engine.getCurrentExecutions()).thenReturn(List.of(mock(ExecutionContext.class)));

        long start = System.currentTimeMillis();
        shutdown(300).close();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue("waited " + elapsed + " ms, expected at least the 300 ms timeout", elapsed >= 300);
        assertTrue("waited " + elapsed + " ms, the timeout must bound the wait", elapsed < 10_000);
        verify(engine).close();
    }

    /**
     * The residual race of the registration shape: if the shutdown hook closes the engine while the command
     * is completing normally, the try-with-resources behind it closes the same engine a second time - after
     * the hook, never concurrently with it, since deregistering takes the hook's monitor. That is only
     * harmless because closing an engine twice is, which is what this pins on the real class rather than on
     * a mock.
     */
    @Test
    public void closingARealEngineTwiceIsSafe() {
        ExecutionEngine realEngine = ExecutionEngine.builder().build();
        AtomicBoolean attributeClosed = new AtomicBoolean();
        realEngine.getExecutionEngineContext().put(ProbeAttribute.class, new ProbeAttribute(attributeClosed));

        realEngine.close();
        realEngine.close();

        assertTrue("the context attributes must have been closed with the engine", attributeClosed.get());
    }

    /**
     * A {@code Closeable} attribute of the engine context, which {@code AbstractContext.close()} closes -
     * the mechanism the apResource cache root and the resource manager rely on.
     */
    private static class ProbeAttribute implements Closeable {
        private final AtomicBoolean closed;

        ProbeAttribute(AtomicBoolean closed) {
            this.closed = closed;
        }

        @Override
        public void close() {
            closed.set(true);
        }
    }

    private void doOnClose(AtomicBoolean flag) {
        doAnswer(invocation -> {
            flag.set(true);
            return null;
        }).when(engine).close();
    }
}
