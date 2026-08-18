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
package step.agents.provisioning.local;

import ch.exense.commons.resilience.RetryHelper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A running local agent process, started by a {@link LocalAgentProvider}.
 * <p>
 * The output of the process is consumed on a dedicated thread. It has to be: a process whose output pipe is never
 * read blocks as soon as the pipe buffer is full. It is logged at debug level and the last lines are additionally
 * retained in memory, which is what allows a start failure to be reported with the reason the agent itself printed
 * rather than with a bare "no token connected within Xs".
 */
public class LocalAgentProcess {

    private static final Logger logger = LoggerFactory.getLogger(LocalAgentProcess.class);
    private static final int RETAINED_OUTPUT_LINES = 50;
    private static final long FORCIBLE_TERMINATION_TIMEOUT_MS = 5000;
    /**
     * How long the end of a stopped process' output is waited for. Bounded, because the output of a process which
     * could not be terminated never ends. Reaching it takes microseconds: the process is gone by then, and what is
     * left to read is what its pipe still holds.
     */
    private static final long OUTPUT_PUMP_TIMEOUT_MS = 1000;
    private static final int WORKING_DIRECTORY_DELETION_RETRIES = 5;
    private static final long WORKING_DIRECTORY_DELETION_RETRY_WAIT_MS = 100;

    private final String name;
    private final Process process;
    private final Path workingDirectory;
    private final boolean printOutput;
    private final Deque<String> lastOutputLines = new ArrayDeque<>();
    private final Thread outputPump;

    /**
     * @param printOutput whether to print what the agent logs. The output is always read — an agent whose output is
     *                    not consumed eventually blocks on a full pipe, and the last lines explain a start failure —
     *                    but printing it is reserved for the verbose mode.
     */
    public LocalAgentProcess(String name, Process process, Path workingDirectory, boolean printOutput) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.process = Objects.requireNonNull(process, "process must not be null");
        this.workingDirectory = Objects.requireNonNull(workingDirectory, "workingDirectory must not be null");
        this.printOutput = printOutput;
        this.outputPump = startOutputPump();
    }

    private Thread startOutputPump() {
        Thread pump = new Thread(this::pumpOutput, name + " output");
        // A daemon on purpose: the pump of a process which survived even a forcible destroy blocks for ever, and must
        // never be what keeps the CLI from exiting. Nothing can stop it from the outside - a blocking read on a
        // process pipe answers neither to Thread.interrupt() nor to closing the stream, which blocks the caller
        // instead on Windows - so the end of the process' output is the only thing that ends it.
        pump.setDaemon(true);
        pump.start();
        return pump;
    }

    private void pumpOutput() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (printOutput) {
                    logger.info("{} -- {}", name, line);
                } else {
                    logger.debug("{} -- {}", name, line);
                }
                synchronized (lastOutputLines) {
                    lastOutputLines.addLast(line);
                    if (lastOutputLines.size() > RETAINED_OUTPUT_LINES) {
                        lastOutputLines.removeFirst();
                    }
                }
            }
        } catch (IOException e) {
            logger.debug("Error while reading the output of {}", name, e);
        }
    }

    public String getName() {
        return name;
    }

    // Package private for the sake of the tests, which check that the pump can never keep this JVM alive
    Thread getOutputPump() {
        return outputPump;
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    /**
     * @return the last lines the agent printed, oldest first. Used to explain a start failure.
     */
    public List<String> getLastOutputLines() {
        synchronized (lastOutputLines) {
            return new ArrayList<>(lastOutputLines);
        }
    }

    /**
     * Terminates the process and deletes its working directory.
     * <p>
     * The caller is expected to have asked the agent to shut down gracefully through the grid beforehand. This method
     * only waits for that shutdown to complete and falls back to killing the process, so that a hanging or already
     * unreachable agent can never keep the CLI from terminating.
     *
     * @param gracefulTimeoutMs how long to wait for the process to terminate on its own. Pass {@code 0} when the
     *                          agent was not asked to shut down, or refused to: there is then nothing to wait for,
     *                          and waiting would only delay the end of the execution by the whole timeout.
     */
    public void stop(long gracefulTimeoutMs) {
        try {
            if (gracefulTimeoutMs > 0 && process.waitFor(gracefulTimeoutMs, TimeUnit.MILLISECONDS)) {
                logger.debug("{} stopped.", name);
            } else {
                if (gracefulTimeoutMs > 0) {
                    logger.warn("{} did not shut down within {}ms. Destroying it forcibly.", name, gracefulTimeoutMs);
                } else {
                    logger.debug("{} cannot be shut down through the grid. Destroying it.", name);
                }
                destroyForciblyWithDescendants();
                // Killing a process is not instantaneous, but it is not a graceful shutdown either: it takes
                // milliseconds, and waiting the full shutdown timeout for it would make no sense.
                if (!process.waitFor(FORCIBLE_TERMINATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    logger.error("{} could not be stopped, even forcibly. Its working directory {} is left behind.",
                        name, workingDirectory);
                    return;
                }
                logger.debug("{} stopped.", name);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for {} to stop. Destroying it forcibly.", name);
            destroyForciblyWithDescendants();
            return;
        } finally {
            awaitOutputPump();
        }
        deleteWorkingDirectory();
    }

    /**
     * Waits for the end of the process' output, so that everything the agent printed has been logged and retained by
     * the time this returns. The pump ends by itself when the process closes its output, which is what terminating it
     * does; the wait is bounded because a process which could not be terminated never closes it, and the pump is then
     * left running as the daemon thread it is.
     */
    private void awaitOutputPump() {
        try {
            outputPump.join(OUTPUT_PUMP_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (outputPump.isAlive()) {
            logger.debug("The output of {} is still being read {}ms after it was stopped. Its process is still running.",
                name, OUTPUT_PUMP_TIMEOUT_MS);
        }
    }

    /**
     * Kills the process together with the processes it started.
     * <p>
     * The agent is not always the process started here: a globally installed Node.js agent is started through the
     * command npm put on the PATH, which on Windows is a command file and therefore runs the agent as a child of
     * cmd.exe. Killing that shell alone would leave the agent behind, holding its port, its registration on the grid
     * and the files of the working directory this method's caller then tries to delete.
     */
    private void destroyForciblyWithDescendants() {
        // Taken before anything is killed: the descendants of a dead process are no longer reachable through it
        List<ProcessHandle> descendants = process.descendants().toList();
        descendants.forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private void deleteWorkingDirectory() {
        try {
            RetryHelper.executeWithRetryOnExceptions(
                () -> {
                    FileUtils.deleteDirectory(workingDirectory.toFile());
                    return null;
                },
                WORKING_DIRECTORY_DELETION_RETRIES,
                WORKING_DIRECTORY_DELETION_RETRY_WAIT_MS,
                List.of(IOException.class),
                "Delete working directory of " + name + " (" + workingDirectory + ")"
            );
        } catch (Exception e) {
            // Not worth failing the execution for: the directory is swept on the next start (see LocalAgentWorkspace).
            logger.warn("Failed to delete the working directory {} of {}.", workingDirectory, name, e);
        }
    }
}
