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
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Covers the installations a user can point {@code --localAgentDotNet} at. None of these tests starts the agent: they
 * are about what the provider accepts and, more importantly, about failing with an actionable message instead of
 * letting a binary die on an incomplete installation.
 */
public class DotNetLocalAgentProviderTest {

    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private static final String AGENT_EXECUTABLE = WINDOWS ? "StepAgent.exe" : "StepAgent";
    private static final String WORKER_EXECUTABLE = WINDOWS ? "StepAgentWorker.exe" : "StepAgentWorker";

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    /**
     * The layout of the published agent distribution, which is what a user unpacks and points at.
     */
    @Test
    public void acceptsTheAgentDistribution() throws Exception {
        Path distribution = installedAgent(folder.getRoot().toPath().resolve("agent"));

        DotNetLocalAgentProvider provider = providerFor(distribution);

        Assert.assertTrue(provider.isAvailable());
        Assert.assertEquals(distribution.resolve("bin"), provider.validateInstallation());
    }

    /**
     * Pointing at the bin directory instead of at the distribution is a reasonable guess: it is the directory the
     * start scripts of the agent run from.
     */
    @Test
    public void acceptsTheBinDirectoryOfTheDistribution() throws Exception {
        Path binDirectory = installedAgent(folder.getRoot().toPath().resolve("agent")).resolve("bin");

        Assert.assertEquals(binDirectory, providerFor(binDirectory).validateInstallation());
    }

    @Test
    public void rejectsADirectoryWhichHoldsNoAgent() throws Exception {
        Path empty = folder.newFolder("empty").toPath();

        DotNetLocalAgentProvider provider = providerFor(empty);

        Assert.assertFalse("An empty directory is not an agent", provider.isAvailable());
        LocalAgentException exception = Assert.assertThrows(LocalAgentException.class, provider::validateInstallation);
        Assert.assertTrue("Should point at the missing executable: " + exception.getMessage(),
            exception.getMessage().contains(AGENT_EXECUTABLE));
    }

    /**
     * An installation of another platform has the executables of that platform: the agent is there, its worker is not.
     */
    @Test
    public void rejectsAnInstallationWithoutItsWorker() throws Exception {
        Path distribution = folder.getRoot().toPath().resolve("agent");
        Files.createDirectories(distribution.resolve("bin"));
        Files.createFile(distribution.resolve("bin").resolve(AGENT_EXECUTABLE));

        LocalAgentException exception = Assert.assertThrows(LocalAgentException.class,
            () -> providerFor(distribution).validateInstallation());
        Assert.assertTrue("Should point at the missing worker: " + exception.getMessage(),
            exception.getMessage().contains(WORKER_EXECUTABLE));
    }

    /**
     * The agent type is not offered at all when the user did not provide an installation, which is what makes a .NET
     * keyword fail with "no agent available" rather than with an obscure process error.
     */
    @Test
    public void isNotAvailableWithoutAConfiguredInstallation() {
        Assume.assumeTrue("The environment variable is the documented fallback and is set on this machine",
            System.getenv(DotNetLocalAgentProvider.AGENT_HOME_ENV_VAR) == null);

        DotNetLocalAgentProvider provider = new DotNetLocalAgentProvider(new LocalAgentProvisioningConfiguration());

        Assert.assertFalse(provider.isAvailable());
        String hint = provider.getInstallationHint();
        Assert.assertTrue(hint, hint.contains("--localAgentDotNet"));
        Assert.assertTrue(hint, hint.contains(DotNetLocalAgentProvider.AGENT_HOME_ENV_VAR));
    }

    /**
     * The hint is the whole explanation an execution requiring this agent type gets, and a user who did configure an
     * installation must be told what is wrong with <b>it</b> rather than to configure one. A Windows path written in a
     * properties file without doubling its backslashes arrives here as exactly such an installation: the path is
     * mangled by the escaping rules of {@code java.util.Properties} and points nowhere.
     */
    @Test
    public void reportsWhatIsWrongWithTheConfiguredInstallation() throws Exception {
        Path notAnAgent = folder.newFolder("CDevAppsStep").toPath();

        String hint = providerFor(notAnAgent).getInstallationHint();

        Assert.assertTrue("Should name the configured installation: " + hint, hint.contains(notAnAgent.toString()));
        Assert.assertTrue("Should say what is missing in it: " + hint, hint.contains(AGENT_EXECUTABLE));
        Assert.assertFalse("Should not ask for what is already configured: " + hint,
            hint.contains(DotNetLocalAgentProvider.AGENT_HOME_ENV_VAR));
    }

    /**
     * @return an agent distribution as the published archive provides it
     */
    private static Path installedAgent(Path distribution) throws IOException {
        Path binDirectory = distribution.resolve("bin");
        Files.createDirectories(binDirectory.resolve("worker"));
        Files.createFile(binDirectory.resolve(AGENT_EXECUTABLE));
        Files.createFile(binDirectory.resolve("worker").resolve(WORKER_EXECUTABLE));
        return distribution;
    }

    private static DotNetLocalAgentProvider providerFor(Path configuredAgent) {
        return new DotNetLocalAgentProvider(new LocalAgentProvisioningConfiguration()
            .setDotNetAgentPath(configuredAgent));
    }
}
