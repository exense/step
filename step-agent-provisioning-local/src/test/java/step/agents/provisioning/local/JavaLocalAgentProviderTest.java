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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import step.agents.provisioning.local.JavaLocalAgentProvider.EmbeddedAgent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Covers when the agent extracted by an earlier run may be run again. The directory it is kept in is named after the
 * Step version, which is the same for every build of one version, so nothing but the recorded identity tells a
 * rebuilt CLI that the agent next to it belongs to the previous build.
 */
public class JavaLocalAgentProviderTest {

    private static final long SIZE = 12;
    private static final EmbeddedAgent EMBEDDED = new EmbeddedAgent(SIZE, "12:3141592653");

    @Rule
    public final TemporaryFolder installedDirectory = new TemporaryFolder();

    private Path agentJar;
    private Path identityFile;

    @Before
    public void extractAnAgent() throws IOException {
        agentJar = writeAgentJar(SIZE);
        identityFile = installedDirectory.getRoot().toPath().resolve("step-agent.jar.id");
        Files.writeString(identityFile, EMBEDDED.identity());
    }

    @Test
    public void anAgentExtractedFromTheEmbeddedOneIsReused() {
        Assert.assertTrue(JavaLocalAgentProvider.isExtractedAgentUsable(agentJar, identityFile, EMBEDDED));
    }

    @Test
    public void anAgentExtractedFromAnotherBuildIsNotReused() {
        EmbeddedAgent rebuilt = new EmbeddedAgent(SIZE, "12:2718281828");

        Assert.assertFalse(JavaLocalAgentProvider.isExtractedAgentUsable(agentJar, identityFile, rebuilt));
    }

    @Test
    public void anAgentWithoutARecordedIdentityIsNotReused() throws IOException {
        Files.delete(identityFile);

        Assert.assertFalse(JavaLocalAgentProvider.isExtractedAgentUsable(agentJar, identityFile, EMBEDDED));
    }

    @Test
    public void aMissingAgentIsNotReused() throws IOException {
        Files.delete(agentJar);

        Assert.assertFalse(JavaLocalAgentProvider.isExtractedAgentUsable(agentJar, identityFile, EMBEDDED));
    }

    /** The identity records what was written, so a jar truncated or replaced since must still be caught. */
    @Test
    public void anAgentOfTheWrongSizeIsNotReused() throws IOException {
        writeAgentJar(SIZE - 1);

        Assert.assertFalse(JavaLocalAgentProvider.isExtractedAgentUsable(agentJar, identityFile, EMBEDDED));
    }

    @Test
    public void anAgentIsNotReusedWhenTheEmbeddedOneCannotBeIdentified() {
        Assert.assertFalse(JavaLocalAgentProvider.isExtractedAgentUsable(agentJar, identityFile, null));
    }

    private Path writeAgentJar(long size) throws IOException {
        Path jar = installedDirectory.getRoot().toPath().resolve("step-agent.jar");
        Files.write(jar, new byte[(int) size]);
        return jar;
    }
}
