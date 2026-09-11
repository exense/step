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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Covers what the provider starts, and what it refuses to. None of these tests starts node: they are about the
 * command the provider resolves and, more importantly, about failing with an actionable message instead of letting
 * node die on a directory holding no agent. The embedded agent, which this module does not carry, is covered end to
 * end by the CLI.
 */
public class NodeLocalAgentProviderTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    /**
     * The package itself: what {@code npm install -g step-node-agent} leaves behind, and what an unpacked archive
     * looks like. Both are started the same way as the embedded agent, through the script.
     */
    @Test
    public void startsTheConfiguredAgentPackage() throws Exception {
        Path agentPackage = folder.getRoot().toPath().resolve("step-node-agent");
        Path script = Files.createFile(Files.createDirectories(agentPackage).resolve("server.js"));

        Assert.assertEquals(List.of("node", script.toAbsolutePath().toString()),
            providerFor(agentPackage).resolveAgentCommand());
    }

    /**
     * The other shape: the project directory where a user ran {@code npm install ./step-node-agent-<version>.tgz},
     * which puts the package under {@code node_modules}.
     */
    @Test
    public void startsTheConfiguredAgentInstalledInAProject() throws Exception {
        Path project = folder.getRoot().toPath().resolve("my-project");
        Path script = installedAgent(project);

        Assert.assertEquals(List.of("node", script.toAbsolutePath().toString()),
            providerFor(project).resolveAgentCommand());
    }

    /**
     * A source checkout, an empty directory, a project where npm was never run: the message has to name both places
     * that were looked at, otherwise there is no telling which of the two shapes was expected.
     */
    @Test
    public void rejectsADirectoryHoldingNoAgent() throws Exception {
        Path empty = folder.newFolder("empty").toPath();

        LocalAgentException exception = Assert.assertThrows(LocalAgentException.class,
            () -> providerFor(empty).resolveAgentCommand());

        Assert.assertTrue(exception.getMessage(),
            exception.getMessage().contains(empty.resolve("server.js").toString()));
        Assert.assertTrue(exception.getMessage(), exception.getMessage()
            .contains(empty.resolve(Path.of("node_modules", "step-node-agent", "server.js")).toString()));
    }

    /**
     * The embedded agent is what makes a local execution work without a registry, so a CLI built without it and no
     * agent configured is a dead end. It has to say so, and say what to do about it.
     */
    @Test
    public void failsWhenNoAgentIsEmbeddedAndNoneIsConfigured() throws Exception {
        // This module does not carry the embedded agent: it is the CLI that embeds it, see the launcher pom
        LocalAgentException exception = Assert.assertThrows(LocalAgentException.class,
            () -> providerFor(null).resolveAgentCommand());

        Assert.assertTrue(exception.getMessage(), exception.getMessage().contains("--localAgentNode"));
    }

    @Test
    public void unpacksAnArchiveIntoTheGivenDirectory() throws Exception {
        Path archive = folder.getRoot().toPath().resolve("agent.tar.gz");
        writeArchive(archive, "server.js", "console.log('agent')");
        Path directory = folder.newFolder("extracted").toPath();

        NodeLocalAgentProvider.unpack(archive.toUri().toURL(), directory);

        Assert.assertEquals("console.log('agent')", Files.readString(directory.resolve("server.js")));
    }

    /**
     * Nothing produces such an archive here, ours being built by our own build, but unpacking one would write
     * anywhere on the machine.
     */
    @Test
    public void refusesAnArchiveWritingOutsideOfTheDirectory() throws Exception {
        Path archive = folder.getRoot().toPath().resolve("evil.tar.gz");
        writeArchive(archive, "../escaped.js", "anything");
        Path directory = folder.newFolder("target").toPath();

        IOException exception = Assert.assertThrows(IOException.class,
            () -> NodeLocalAgentProvider.unpack(archive.toUri().toURL(), directory));

        Assert.assertTrue(exception.getMessage(), exception.getMessage().contains("outside"));
        Assert.assertFalse(Files.exists(directory.getParent().resolve("escaped.js")));
    }

    private static void writeArchive(Path archive, String entryName, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = Files.newOutputStream(archive);
             TarArchiveOutputStream tar = new TarArchiveOutputStream(new GzipCompressorOutputStream(out))) {
            // The names of the entries an npm project produces are longer than the 100 characters tar allows by
            // default, which is what the embedded agent is packed with too
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            TarArchiveEntry entry = new TarArchiveEntry(entryName);
            entry.setSize(bytes.length);
            tar.putArchiveEntry(entry);
            tar.write(bytes);
            tar.closeArchiveEntry();
        }
    }

    /**
     * Installs the agent into a project the way npm does, as far as this provider is concerned.
     *
     * @return the script the provider is expected to start
     */
    private static Path installedAgent(Path project) throws IOException {
        Path packageDirectory = project.resolve("node_modules").resolve("step-node-agent");
        Files.createDirectories(packageDirectory);
        return Files.createFile(packageDirectory.resolve("server.js"));
    }

    private NodeLocalAgentProvider providerFor(Path configuredAgent) throws IOException {
        LocalAgentProvisioningConfiguration configuration = new LocalAgentProvisioningConfiguration()
            .setNodeAgentPath(configuredAgent)
            .setWorkDirectory(folder.newFolder().toPath());
        return new NodeLocalAgentProvider(configuration, new LocalAgentWorkspace(configuration.getWorkDirectory()));
    }
}
