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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Covers what the workspace sweeps when it is created: the run directories of a CLI which never got to clean up, and
 * nothing else, the installed agents and libraries living in the same place.
 */
public class LocalAgentWorkspaceTest {

    @Rule
    public final TemporaryFolder workDirectory = new TemporaryFolder();

    @Test
    public void sweepsTheRunDirectoriesOfAPreviousRun() throws IOException {
        Path agentDirectory = directory("agent-java-123");
        Path gridDirectory = directory("grid-456");

        new LocalAgentWorkspace(workDirectory.getRoot().toPath());

        Assert.assertFalse(Files.exists(agentDirectory));
        Assert.assertFalse(Files.exists(gridDirectory));
    }

    @Test
    public void keepsTheDirectoriesWhichAreNotRunDirectories() throws IOException {
        Path installedAgents = directory("agents");
        Path installedLibraries = directory("libraries");

        new LocalAgentWorkspace(workDirectory.getRoot().toPath());

        Assert.assertTrue(Files.isDirectory(installedAgents));
        Assert.assertTrue(Files.isDirectory(installedLibraries));
    }

    /**
     * Only directories are swept: a run directory is one, and a file named like one is somebody else's.
     */
    @Test
    public void keepsTheFilesNamedLikeARunDirectory() throws IOException {
        Path file = Files.writeString(workDirectory.getRoot().toPath().resolve("agent-java-123.log"), "content");

        new LocalAgentWorkspace(workDirectory.getRoot().toPath());

        Assert.assertTrue(Files.isRegularFile(file));
    }

    private Path directory(String name) throws IOException {
        return Files.createDirectory(workDirectory.getRoot().toPath().resolve(name));
    }
}
