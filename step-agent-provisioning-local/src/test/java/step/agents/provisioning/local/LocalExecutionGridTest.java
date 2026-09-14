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
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

/**
 * Covers the lifetime of the files the embedded grid caches. They are not a temporary detail: the file manager stores
 * there everything sent to the agents, keyword libraries included, so leaving them behind fills the temporary
 * directory of the user with every local execution they run.
 */
public class LocalExecutionGridTest {

    @Rule
    public final TemporaryFolder workDirectory = new TemporaryFolder();

    @Test
    public void deletesTheFilesOfItsFileManagerWhenStopped() throws Exception {
        LocalAgentWorkspace workspace = new LocalAgentWorkspace(workDirectory.getRoot().toPath());

        Path fileManagerDirectory;
        try (LocalExecutionGrid grid = new LocalExecutionGrid(Duration.ofSeconds(10), workspace)) {
            Assert.assertNotNull(grid.getGridUrl());
            fileManagerDirectory = gridDirectories().stream().findFirst().orElseThrow();
            // A cached file, as the file manager leaves them behind: an empty directory would be deleted by far less
            Files.writeString(fileManagerDirectory.resolve("aCachedFile"), "content");
        }

        Assert.assertFalse("The file manager directory should have been deleted with the grid",
            Files.exists(fileManagerDirectory));
    }

    /**
     * A CLI which is killed never stops its grid. The next local execution sweeps what it left behind, the same way it
     * sweeps the directories of agents which were never stopped.
     */
    @Test
    public void sweepsTheFilesLeftBehindByAKilledRun() throws Exception {
        Path staleDirectory = Files.createDirectory(workDirectory.getRoot().toPath().resolve("grid-fromAKilledRun"));
        Files.writeString(staleDirectory.resolve("aCachedFile"), "content");

        new LocalAgentWorkspace(workDirectory.getRoot().toPath());

        Assert.assertFalse("The file manager directory of the previous run should have been swept",
            Files.exists(staleDirectory));
    }

    private List<Path> gridDirectories() throws IOException {
        try (Stream<Path> entries = Files.list(workDirectory.getRoot().toPath())) {
            return entries.filter(Files::isDirectory)
                .filter(path -> path.getFileName().toString().startsWith("grid-")).toList();
        }
    }
}
