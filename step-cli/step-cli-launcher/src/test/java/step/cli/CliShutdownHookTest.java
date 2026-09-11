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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * The registry behind {@link CliShutdownHook}, exercised without the JVM hook it could not otherwise
 * trigger: the hook thread is captured instead of registered, and {@code onShutdown} is called directly.
 */
public class CliShutdownHookTest {

    private final List<Thread> registeredHooks = new ArrayList<>();
    private final CliShutdownHook shutdownHook = new CliShutdownHook(registeredHooks::add);

    /**
     * A named resource that records the order in which the resources of one test were closed.
     */
    private static class Recorded implements AutoCloseable {
        private final String name;
        private final List<String> closed;

        Recorded(String name, List<String> closed) {
            this.name = name;
            this.closed = closed;
        }

        @Override
        public void close() {
            closed.add(name);
        }
    }

    @Test
    public void closesWhatIsStillRegisteredWhenTheProcessIsInterrupted() {
        List<String> closed = new ArrayList<>();
        shutdownHook.add(new Recorded("engine", closed));

        shutdownHook.onShutdown();

        assertEquals(List.of("engine"), closed);
    }

    /**
     * What was built later may depend on what was built earlier, so the later registration is closed
     * first - the ordering {@code AbstractContext.close()} does not give within one resource.
     */
    @Test
    public void closesInReverseRegistrationOrder() {
        List<String> closed = new ArrayList<>();
        shutdownHook.add(new Recorded("first", closed));
        shutdownHook.add(new Recorded("second", closed));
        shutdownHook.add(new Recorded("third", closed));

        shutdownHook.onShutdown();

        assertEquals(List.of("third", "second", "first"), closed);
    }

    /**
     * The normal path: the resource is closed by the try-with-resources that owns it, and the registration
     * only takes it back out of the registry, so the hook has nothing left to close. Nothing here closes
     * the resource on its behalf.
     */
    @Test
    public void theRegistrationOnlyDeregisters() {
        List<String> closed = new ArrayList<>();
        try (CliShutdownHook.Registration ignored = shutdownHook.add(new Recorded("engine", closed))) {
            assertEquals(List.of(), closed);
        }

        assertEquals("deregistering must not close anything", List.of(), closed);

        shutdownHook.onShutdown();

        assertEquals("and the hook must no longer hold it", List.of(), closed);
    }

    @Test
    public void deregisteringTwiceIsANoOp() {
        List<String> closed = new ArrayList<>();
        CliShutdownHook.Registration registration = shutdownHook.add(new Recorded("engine", closed));

        registration.close();
        registration.close();
        shutdownHook.onShutdown();

        assertEquals(List.of(), closed);
    }

    /**
     * A resource the hook already closed, because the process was interrupted while the command was still
     * running, is no longer registered: deregistering it behind the hook changes nothing, and the
     * try-with-resources closing it a second time is the resource's own problem - which is why the close of
     * anything registered here has to be idempotent.
     */
    @Test
    public void deregisteringAfterTheHookAlreadyClosedItIsANoOp() {
        List<String> closed = new ArrayList<>();
        CliShutdownHook.Registration registration = shutdownHook.add(new Recorded("engine", closed));

        shutdownHook.onShutdown();
        registration.close();

        assertEquals(List.of("engine"), closed);
    }

    /**
     * On shutdown there is nobody left to tell, and the resources registered before the failing one still
     * have to be closed.
     */
    @Test
    public void aFailingCloseDoesNotStopTheShutdown() {
        List<String> closed = new ArrayList<>();
        shutdownHook.add(new Recorded("first", closed));
        shutdownHook.add(() -> {
            throw new IllegalStateException("cannot close");
        });

        shutdownHook.onShutdown();

        assertEquals(List.of("first"), closed);
    }

    /**
     * A command owning nothing - {@code --help}, a remote deployment - must install no hook at all.
     */
    @Test
    public void theHookIsRegisteredOnceAndOnlyWhenSomethingNeedsIt() {
        assertEquals(List.of(), registeredHooks);

        shutdownHook.add(new Recorded("first", new ArrayList<>()));
        shutdownHook.add(new Recorded("second", new ArrayList<>()));

        assertEquals(1, registeredHooks.size());
        assertEquals(CliShutdownHook.THREAD_NAME, registeredHooks.get(0).getName());
    }

    @Test
    public void theShutdownOfACommandThatRegisteredNothingDoesNothing() {
        shutdownHook.onShutdown();

        assertEquals(List.of(), registeredHooks);
    }

    @Test
    public void aNullResourceIsRefused() {
        assertThrows(NullPointerException.class, () -> shutdownHook.add(null));

        assertEquals("nothing was registered, so no hook was installed", List.of(), registeredHooks);
    }

    /**
     * The registered thread is what the JVM runs on shutdown: it must call back into the registry.
     */
    @Test
    public void theRegisteredThreadRunsTheShutdown() throws InterruptedException {
        List<String> closed = new ArrayList<>();
        shutdownHook.add(new Recorded("engine", closed));

        Thread hook = registeredHooks.get(0);
        assertEquals("the hook is registered, not started", Thread.State.NEW, hook.getState());

        hook.start();
        hook.join();

        assertEquals(List.of("engine"), closed);
    }
}
