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
import java.util.List;

/**
 * Covers what the provider starts, and what it refuses to. None of these tests starts node: they are about the
 * command the provider resolves and, more importantly, about failing with an actionable message instead of letting
 * node die on a directory holding no agent.
 */
public class NodeLocalAgentProviderTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    /**
     * What a user points {@code --localAgentNode} at: the project directory where they ran
     * {@code npm install ./step-node-agent-<version>.tgz}, which is where npm generated the command starting it.
     */
    @Test
    public void startsTheAgentInstalledInTheConfiguredProject() throws Exception {
        Path project = folder.getRoot().toPath().resolve("my-project");
        Path command = installedAgent(project);

        Assert.assertEquals(List.of(command.toString()), providerFor(project).resolveAgentCommand());
    }

    /**
     * A source checkout, a directory holding the agent package but no installation, an empty directory: all the same
     * thing here, an installation that was never made. The message has to say what was expected and how to get it.
     */
    @Test
    public void rejectsADirectoryWithNoInstalledAgent() throws Exception {
        Path empty = folder.newFolder("empty").toPath();

        LocalAgentException exception = Assert.assertThrows(LocalAgentException.class,
            () -> providerFor(empty).resolveAgentCommand());
        Assert.assertTrue("Should name the expected command: " + exception.getMessage(),
            exception.getMessage().contains(Path.of("node_modules", ".bin",
                NodeLocalAgentProvider.OsCommands.STEP_NODE_AGENT).toString()));
        Assert.assertTrue("Should say how to install the agent: " + exception.getMessage(),
            exception.getMessage().contains("npm install"));
    }

    /**
     * The package unpacked next to a {@code node_modules} of its own is not an installation either: only the command
     * npm generates says that npm ran.
     */
    @Test
    public void rejectsAnUnpackedPackageWhichWasNeverInstalled() throws Exception {
        Path checkout = folder.getRoot().toPath().resolve("step-node-agent");
        Files.createDirectories(checkout.resolve("node_modules").resolve("express"));
        Files.createFile(checkout.resolve("server.js"));

        Assert.assertThrows(LocalAgentException.class, () -> providerFor(checkout).resolveAgentCommand());
    }

    /**
     * An {@code npm install -g step-node-agent} is started through the command it puts on the PATH, the CLI neither
     * locating nor installing anything of its own.
     */
    @Test
    public void startsAGloballyInstalledAgentThroughItsCommand() throws Exception {
        Assert.assertEquals(List.of(NodeLocalAgentProvider.OsCommands.STEP_NODE_AGENT),
            provider(null, true).resolveAgentCommand());
    }

    /**
     * A user pointing at an agent means that one, whatever is installed globally.
     */
    @Test
    public void prefersTheConfiguredAgentOverTheGloballyInstalledOne() throws Exception {
        Path project = folder.getRoot().toPath().resolve("my-project");
        Path command = installedAgent(project);

        Assert.assertEquals(List.of(command.toString()), provider(project, true).resolveAgentCommand());
    }

    /**
     * Installs an agent into a project the way npm does, as far as this provider is concerned: the generated command.
     *
     * @return the command npm generated, which is what the provider is expected to start
     */
    private static Path installedAgent(Path project) throws IOException {
        Path binDirectory = project.resolve("node_modules").resolve(".bin");
        Files.createDirectories(binDirectory);
        return Files.createFile(binDirectory.resolve(NodeLocalAgentProvider.OsCommands.STEP_NODE_AGENT))
            .toAbsolutePath();
    }

    private NodeLocalAgentProvider providerFor(Path configuredAgent) throws IOException {
        return provider(configuredAgent, false);
    }

    /**
     * @param globallyInstalled what to report instead of asking npm whether the agent is installed globally, which
     *                          keeps these tests independent from what the machine running them has installed
     */
    private NodeLocalAgentProvider provider(Path configuredAgent, boolean globallyInstalled) throws IOException {
        LocalAgentProvisioningConfiguration configuration = new LocalAgentProvisioningConfiguration()
            .setNodeAgentPath(configuredAgent)
            .setWorkDirectory(folder.newFolder().toPath());
        return new NodeLocalAgentProvider(configuration, new LocalAgentWorkspace(configuration.getWorkDirectory())) {
            @Override
            boolean lookupGlobalInstallation() {
                return globallyInstalled;
            }
        };
    }
}
