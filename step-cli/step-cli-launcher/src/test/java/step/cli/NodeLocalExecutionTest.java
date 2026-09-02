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
package step.cli;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import step.agents.provisioning.local.LocalAgentProvisioningConfiguration;
import step.agents.provisioning.local.LocalAgentWorkspace;
import step.agents.provisioning.local.NodeLocalAgentProvider;
import step.junit.categories.LocalNode;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Runs an automation package whose only keyword is a Node.js one.
 * <p>
 * A Node.js keyword can only run on a Node.js agent: there is no in-JVM handler for it, so this is the test that
 * proves keywords are really routed to the agents the CLI provisions. It is the counterpart of
 * {@link ApLocalExecuteCommandHandlerTest}, which cannot prove it — a Java keyword runs in the CLI's own JVM just as
 * happily as on an agent, so it passed even while every keyword was silently bypassing the agents.
 * <p>
 * The package is assembled here rather than committed as a jar, so that the keyword and the plan stay readable and
 * modifiable in the sources.
 * <p>
 * <b>Requires a Node.js agent installed on the machine</b>, hence the category excluding it by default. The CLI
 * installs {@code step-node-agent} at its own version, which during development is the release being worked towards
 * and is not published yet; the snapshot of the branch is published after the maven build, so it does not exist while
 * the tests run either. A global installation ({@code npm install -g step-node-agent}) is used in preference to
 * installing one and is what makes this test runnable, {@code --localAgentNode} being the other way.
 * <p>
 * To run it: {@code mvn test -Dexcluded.node.group= -Dtest=NodeLocalExecutionTest}.
 */
@Category(LocalNode.class)
public class NodeLocalExecutionTest {

    private static final String KEYWORD_LIBRARY_ARCHIVE = "node-keyword-library.zip";

    @Rule
    public final TemporaryFolder workDirectory = new TemporaryFolder();

    @Test
    public void executesANodeKeywordOnALocalNodeAgent() throws Exception {
        LocalAgentProvisioningConfiguration configuration = new LocalAgentProvisioningConfiguration()
            .setWorkDirectory(workDirectory.getRoot().toPath())
            // The agent is installed with npm on first use, which downloads it from the registry
            .setAgentStartTimeout(Duration.ofMinutes(5));

        Assume.assumeTrue("Node.js is not available on this machine", new NodeLocalAgentProvider(configuration,
            new LocalAgentWorkspace(configuration.getWorkDirectory())).isAvailable());

        File automationPackage = buildAutomationPackage();

        // The execution itself is the assertion: the handler reports a failed plan by throwing, and the only way this
        // plan can pass is a Node.js keyword having run on a Node.js agent. Where that agent came from is deliberately
        // not asserted: the CLI uses a globally installed step-node-agent when there is one, and only installs it into
        // the workspace otherwise, so both are a correct outcome.
        new ApLocalExecuteCommandHandler()
            .execute(automationPackage, null, null, null, null, null, Map.of(), configuration);
    }

    /**
     * Packs {@code src/test/resources/node-automation-package} into an automation package.
     * <p>
     * A Node.js keyword is not a single script but an npm project, which an automation package carries as a zip: the
     * agent unpacks it, runs {@code npm install} in it and forks a node process there. The library of this package
     * declares no dependency, so that install needs no network access.
     */
    private File buildAutomationPackage() throws IOException {
        Path sources = Path.of("src/test/resources/node-automation-package");
        Path keywordLibrary = workDirectory.getRoot().toPath().resolve(KEYWORD_LIBRARY_ARCHIVE);
        zip(sources.resolve("keyword-library"), keywordLibrary);

        File automationPackage = workDirectory.newFile("node-automation-package.zip");
        try (OutputStream out = Files.newOutputStream(automationPackage.toPath());
             ZipOutputStream zip = new ZipOutputStream(out)) {
            addEntry(zip, sources.resolve("automation-package.yml"), "automation-package.yml");
            addEntry(zip, keywordLibrary, KEYWORD_LIBRARY_ARCHIVE);
        }
        return automationPackage;
    }

    private static void zip(Path directory, Path archive) throws IOException {
        try (OutputStream out = Files.newOutputStream(archive);
             ZipOutputStream zip = new ZipOutputStream(out);
             Stream<Path> files = Files.walk(directory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                // The agent expects the project at the root of the archive, package.json included
                addEntry(zip, file, directory.relativize(file).toString().replace('\\', '/'));
            }
        }
    }

    private static void addEntry(ZipOutputStream zip, Path file, String name) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        Files.copy(file, zip);
        zip.closeEntry();
    }
}
