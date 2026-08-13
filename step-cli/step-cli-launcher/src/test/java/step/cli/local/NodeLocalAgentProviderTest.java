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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Covers the agent installations a user can point {@code --localAgentNode} at. None of these tests starts node: they
 * are about what the provider accepts and, more importantly, about failing with an actionable message instead of
 * letting node die on a broken installation.
 */
public class NodeLocalAgentProviderTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void acceptsTheAgentPackageItself() throws Exception {
        Path agent = installedAgent(folder.getRoot().toPath().resolve("agent"));

        Assert.assertEquals(agent, providerFor(agent).resolveAgentDirectory());
    }

    /**
     * The layout of an {@code npm install --prefix} and of a global install, and the one this provider uses for its
     * own installations. A user pointing at the prefix rather than at the package is making a reasonable guess.
     */
    @Test
    public void acceptsADirectoryContainingTheAgentInNodeModules() throws Exception {
        Path prefix = folder.getRoot().toPath().resolve("prefix");
        Path agent = installedAgent(prefix.resolve("node_modules").resolve("step-node-agent"));

        Assert.assertEquals(agent, providerFor(prefix).resolveAgentDirectory());
    }

    /**
     * npm is free to hoist the dependencies of the agent to the installation prefix instead of leaving them in the
     * package, which must not be mistaken for a package without dependencies.
     */
    @Test
    public void acceptsDependenciesHoistedToTheInstallationPrefix() throws Exception {
        Path prefix = folder.getRoot().toPath().resolve("prefix");
        Path agent = prefix.resolve("node_modules").resolve("step-node-agent");
        Files.createDirectories(agent);
        Files.createFile(agent.resolve("server.js"));
        Files.createDirectories(prefix.resolve("node_modules").resolve("express"));

        Assert.assertEquals(agent, providerFor(agent).resolveAgentDirectory());
    }

    @Test
    public void rejectsASourceCheckoutWithoutInstalledDependencies() throws Exception {
        Path checkout = folder.getRoot().toPath().resolve("step-node-agent");
        Files.createDirectories(checkout);
        Files.createFile(checkout.resolve("server.js"));

        LocalAgentException exception = Assert.assertThrows(LocalAgentException.class,
            () -> providerFor(checkout).resolveAgentDirectory());
        Assert.assertTrue("Should point at the missing dependencies: " + exception.getMessage(),
            exception.getMessage().contains("npm install"));
    }

    @Test
    public void rejectsADirectoryWhichHoldsNoAgent() throws Exception {
        Path empty = folder.newFolder("empty").toPath();

        LocalAgentException exception = Assert.assertThrows(LocalAgentException.class,
            () -> providerFor(empty).resolveAgentDirectory());
        Assert.assertTrue("Should point at the missing main script: " + exception.getMessage(),
            exception.getMessage().contains("server.js"));
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
        Path configured = installedAgent(folder.getRoot().toPath().resolve("agent"));

        Assert.assertEquals(
            List.of(NodeLocalAgentProvider.OsCommands.NODE, configured.resolve("server.js").toAbsolutePath().toString()),
            provider(configured, true).resolveAgentCommand());
    }

    /**
     * @return an agent package as an installation provides it, i.e. with its dependencies
     */
    private static Path installedAgent(Path packageRoot) throws IOException {
        Files.createDirectories(packageRoot);
        Files.createFile(packageRoot.resolve("server.js"));
        Files.createDirectories(packageRoot.resolve("node_modules").resolve("express"));
        return packageRoot;
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
