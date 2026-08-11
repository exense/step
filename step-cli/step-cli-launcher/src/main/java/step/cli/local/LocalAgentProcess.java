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
package step.cli.local;

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
import java.util.concurrent.CompletableFuture;
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
    private static final int WORKING_DIRECTORY_DELETION_RETRIES = 5;
    private static final long WORKING_DIRECTORY_DELETION_RETRY_WAIT_MS = 100;

    private final String name;
    private final Process process;
    private final Path workingDirectory;
    private final Deque<String> lastOutputLines = new ArrayDeque<>();
    private final CompletableFuture<Void> outputPump;

    public LocalAgentProcess(String name, Process process, Path workingDirectory) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.process = Objects.requireNonNull(process, "process must not be null");
        this.workingDirectory = Objects.requireNonNull(workingDirectory, "workingDirectory must not be null");
        this.outputPump = startOutputPump();
    }

    private CompletableFuture<Void> startOutputPump() {
        return CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("{} -- {}", name, line);
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
        });
    }

    public String getName() {
        return name;
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
     * @param shutdownTimeoutMs how long to wait for the process to terminate on its own, and again for it to
     *                          terminate after having been killed
     */
    public void stop(long shutdownTimeoutMs) {
        try {
            if (!process.waitFor(shutdownTimeoutMs, TimeUnit.MILLISECONDS)) {
                logger.warn("{} did not shut down within {}ms. Destroying it forcibly.", name, shutdownTimeoutMs);
                process.destroyForcibly();
                if (!process.waitFor(shutdownTimeoutMs, TimeUnit.MILLISECONDS)) {
                    logger.error("{} could not be stopped, even forcibly. Its working directory {} is left behind.",
                        name, workingDirectory);
                    return;
                }
            }
            logger.debug("{} stopped.", name);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for {} to stop. Destroying it forcibly.", name);
            process.destroyForcibly();
            return;
        } finally {
            // The reader terminates on its own once the process closes its output stream, this is only to make sure
            // the thread is not left running should that not happen.
            outputPump.cancel(true);
        }
        deleteWorkingDirectory();
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
