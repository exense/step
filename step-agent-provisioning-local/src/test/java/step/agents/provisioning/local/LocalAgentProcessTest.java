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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Covers the reading of an agent's output on a real process. The process is this JVM's own java executable printing
 * its version: every machine able to run these tests has one, and it writes to its output and terminates on its own.
 */
public class LocalAgentProcessTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void readsTheOutputOfTheProcessAndRetainsItsLastLines() throws Exception {
        Path workingDirectory = folder.newFolder("agent").toPath();
        Process process = new ProcessBuilder(javaExecutable(), "-version").redirectErrorStream(true).start();

        LocalAgentProcess agent = new LocalAgentProcess("Test agent", process, workingDirectory, false);

        // Not the common ForkJoinPool, and a daemon: the output of an agent which cannot be killed is read for ever,
        // which must neither occupy a pool shared with the rest of this JVM nor keep it from exiting.
        Assert.assertTrue("The output must be read on a daemon thread", agent.getOutputPump().isDaemon());
        Assert.assertEquals("Test agent output", agent.getOutputPump().getName());

        Assert.assertTrue("The process should have terminated on its own", process.waitFor(30, TimeUnit.SECONDS));
        agent.stop(0);

        List<String> output = agent.getLastOutputLines();
        Assert.assertFalse("The output of the process should have been retained: " + output, output.isEmpty());
        Assert.assertTrue("The retained output should be what the process printed: " + output,
            output.stream().anyMatch(line -> line.toLowerCase().contains("version")));
        // stop() waits for the end of the output of a process which has terminated
        Assert.assertFalse("The output should have been read to its end", agent.getOutputPump().isAlive());
        Assert.assertFalse("The working directory should have been deleted", Files.exists(workingDirectory));
    }

    private static String javaExecutable() {
        return ProcessHandle.current().info().command().orElseThrow(
            () -> new IllegalStateException("Unable to determine the java executable of this JVM"));
    }
}
