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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The one place a CLI command hands over something that must be closed even when the process is
 * interrupted.
 * <p>
 * A registration here is only the second half. The resource keeps its own close path - a
 * try-with-resources, as it would have had anyway - and the returned {@link Registration} is the handle
 * that takes it back out again, so the hook is a pure fallback holding exactly what the normal path has
 * not closed yet.
 * <p>
 * Deregistering takes the same monitor the hook does, so a resource can never be closed by both at once:
 * a normal close that arrives while the hook is running waits for it. It can still arrive <i>after</i>,
 * which is why the close of a registered resource has to be idempotent - {@code ExecutionEngine.close()}
 * is, and {@code GracefulEngineShutdownTest} pins it.
 * <p>
 * Resources are closed in reverse registration order, on the assumption that what was built later may
 * depend on what was built earlier. Note that this orders the <i>registered</i> resources only: it says
 * nothing about the order in which a resource closes what it owns internally - {@code AbstractContext.close()},
 * for one, closes the attributes of an execution engine context in unspecified order.
 * <p>
 * The hook itself is registered lazily, on the first registration, so that a command owning nothing -
 * {@code --help}, a remote deployment - installs nothing.
 */
public class CliShutdownHook {

    private static final Logger log = LoggerFactory.getLogger(CliShutdownHook.class);

    static final String THREAD_NAME = "step-cli-shutdown-hook";

    private static final CliShutdownHook INSTANCE = new CliShutdownHook(hook -> Runtime.getRuntime().addShutdownHook(hook));

    /**
     * The handle of a registered resource: closing it deregisters, nothing more. Closing the resource
     * itself stays where it was, in the try-with-resources that owns it.
     */
    public interface Registration extends AutoCloseable {
        @Override
        void close();
    }

    private final Consumer<Thread> hookRegistrar;
    private final Deque<AutoCloseable> resources = new ArrayDeque<>();
    private boolean hookRegistered;

    /**
     * @param hookRegistrar how the shutdown hook reaches the JVM; injected so that a test can exercise
     *                      the registry without a hook it could never trigger
     */
    CliShutdownHook(Consumer<Thread> hookRegistrar) {
        this.hookRegistrar = Objects.requireNonNull(hookRegistrar, "hookRegistrar must not be null");
    }

    /**
     * Registers a resource to be closed if - and only if - the process is interrupted before its own
     * close runs.
     *
     * @return the handle that deregisters it again, meant to be held by the try-with-resources that
     * already owns the resource
     */
    public static Registration register(AutoCloseable resource) {
        return INSTANCE.add(resource);
    }

    synchronized Registration add(AutoCloseable resource) {
        Objects.requireNonNull(resource, "resource must not be null");
        if (!hookRegistered) {
            hookRegistrar.accept(new Thread(this::onShutdown, THREAD_NAME));
            hookRegistered = true;
        }
        resources.push(resource);
        return () -> deregister(resource);
    }

    /**
     * Synchronized like {@link #onShutdown}, which is what keeps the normal close of a resource from
     * running while the hook is closing that same resource: this blocks until the hook is done.
     */
    private synchronized void deregister(AutoCloseable resource) {
        resources.remove(resource);
    }

    /**
     * Closes whatever the normal path has not deregistered yet.
     */
    synchronized void onShutdown() {
        if (resources.isEmpty()) {
            return;
        }
        log.info("Shutdown hook called. Closing {} resource(s)...", resources.size());
        while (!resources.isEmpty()) {
            AutoCloseable resource = resources.pop();
            try {
                resource.close();
            } catch (Exception e) {
                // Best effort: the resources registered before this one still have to be closed
                log.error("Error while closing {} on shutdown", resource.getClass().getName(), e);
            }
        }
    }
}
