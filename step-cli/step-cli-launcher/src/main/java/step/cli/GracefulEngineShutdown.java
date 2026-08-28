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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineRunner;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Closes an {@link ExecutionEngine} that may still be running something - which is the situation of an
 * interrupted CLI run, and the reason a plain {@code close()} is not enough there:
 * <p>
 * Closing an engine under a live execution tears down what that execution is still using which fails.
 * <p>
 * On the normal path there is nothing running - the CLI runs its plans synchronously - so this degrades to
 * exactly the {@code engine.close()} it wraps.
 */
public class GracefulEngineShutdown implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(GracefulEngineShutdown.class);

    /**
     * How long the executions are given to unwind. The agent allows 30 s, but it is draining a server; this
     * runs while somebody is waiting at a terminal for their Ctrl-C to take effect, and an abort only has to
     * reach the next artefact boundary. The wait ends as soon as the executions do, so the timeout is only
     * ever paid by a keyword that ignores its abort.
     */
    static final long DEFAULT_TIMEOUT_MS = 30_000;

    private static final long POLL_INTERVAL_MS = 100;

    private final ExecutionEngine engine;
    private final long timeoutMs;
    private final Consumer<ExecutionContext> abort;

    public GracefulEngineShutdown(ExecutionEngine engine) {
        this(engine, DEFAULT_TIMEOUT_MS, ExecutionEngineRunner::abort);
    }

    GracefulEngineShutdown(ExecutionEngine engine, long timeoutMs, Consumer<ExecutionContext> abort) {
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
        this.timeoutMs = timeoutMs;
        this.abort = Objects.requireNonNull(abort, "abort must not be null");
    }

    @Override
    public void close() {
        abortRunningExecutions();
        engine.close();
    }

    private void abortRunningExecutions() {
        List<ExecutionContext> running = engine.getCurrentExecutions();
        if (running.isEmpty()) {
            return;
        }
        log.info("Aborting {} running execution(s) before closing the execution engine...", running.size());
        running.forEach(abort);
        if (awaitCompletion()) {
            log.info("All executions ended");
        } else {
            log.warn("Timeout after {} ms while waiting for the executions to end. Closing the execution engine "
                + "anyway - a file still held by a running keyword may fail to be deleted.", timeoutMs);
        }
    }

    private boolean awaitCompletion() {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!engine.getCurrentExecutions().isEmpty()) {
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                // Somebody wants this thread to stop waiting; closing the engine is still the right next step
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }
}
