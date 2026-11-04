/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import step.agents.provisioning.local.LocalAgentProvisioningConfiguration;
import step.agents.provisioning.local.LocalAgentWorkspace;
import step.agents.provisioning.local.NodeLocalAgentProvider;
import step.cli.ExecuteAutomationPackageTool.Report;
import step.cli.ExecuteAutomationPackageTool.ReportOutputMode;
import step.cli.ExecuteAutomationPackageTool.ReportType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Executes keywords streaming a file while they run, on local agents started as separate processes, and checks that
 * the file is saved with the other attachments.
 */
public class StreamingAttachmentsLocalExecutionTest {

    private static final List<Report> TREE_REPORT = List.of(new Report(ReportType.tree, List.of(ReportOutputMode.file)));
    private static final String NODE_KEYWORD_LIBRARY_ARCHIVE = "node-keyword-library.zip";

    @Rule
    public final TemporaryFolder workDirectory = new TemporaryFolder();

    @Test
    public void savesTheFileStreamedByAJavaKeyword() throws Exception {
        File reportDir = execute(buildJavaAutomationPackage(), new LocalAgentProvisioningConfiguration()
            .setWorkDirectory(workDirectory.getRoot().toPath()));

        Assert.assertEquals(StreamingKeywords.STREAMED_CONTENT, Files.readString(findAttachment(reportDir, StreamingKeywords.STREAMED_FILE_NAME)));
        Assert.assertEquals(StreamingKeywords.ATTACHED_CONTENT, Files.readString(findAttachment(reportDir, StreamingKeywords.ATTACHED_FILE_NAME)));
    }

    @Test
    public void savesTheFileStreamedByANodeKeyword() throws Exception {
        LocalAgentProvisioningConfiguration configuration = new LocalAgentProvisioningConfiguration()
            .setWorkDirectory(workDirectory.getRoot().toPath())
            // The agent is installed with npm on first use, which downloads it from the registry
            .setAgentStartTimeout(Duration.ofMinutes(5));
        Assume.assumeTrue("Node.js is not available on this machine", new NodeLocalAgentProvider(configuration,
            new LocalAgentWorkspace(configuration.getWorkDirectory())).isAvailable());

        File reportDir = execute(buildNodeAutomationPackage(), configuration);

        Assert.assertEquals("line 1\nline 2\n", Files.readString(findAttachment(reportDir, "node-keyword.log")));
    }

    private File execute(File automationPackage, LocalAgentProvisioningConfiguration configuration) {
        File reportDir = new File(workDirectory.getRoot(), "reports");
        // Fails the test if the plan fails, the handler reports plan failures by throwing
        new ApLocalExecuteCommandHandler()
            .execute(automationPackage, null, null, null, null, null, Map.of(), configuration, TREE_REPORT, reportDir, true);
        return reportDir;
    }

    private static Path findAttachment(File reportDir, String fileName) throws IOException {
        try (Stream<Path> files = Files.walk(reportDir.toPath())) {
            Optional<Path> attachment = files.filter(file -> file.getFileName().toString().equals(fileName)).findFirst();
            Assert.assertTrue("The attachment " + fileName + " should have been saved in " + reportDir, attachment.isPresent());
            return attachment.get();
        }
    }

    /**
     * An automation package of the compiled {@link StreamingKeywords}, with a plan calling its keyword
     */
    private File buildJavaAutomationPackage() throws IOException {
        File automationPackage = workDirectory.newFile("java-streaming-automation-package.jar");
        String className = StreamingKeywords.class.getName().replace('.', '/') + ".class";
        try (OutputStream out = Files.newOutputStream(automationPackage.toPath());
             ZipOutputStream zip = new ZipOutputStream(out);
             InputStream keywordClass = StreamingKeywords.class.getClassLoader().getResourceAsStream(className)) {
            zip.putNextEntry(new ZipEntry("automation-package.yml"));
            zip.write(String.join("\n",
                "version: 1.3.0",
                "name: \"Java streaming attachments\"",
                "plans:",
                "  - name: \"Java streaming plan\"",
                "    root:",
                "      testCase:",
                "        children:",
                "          - callKeyword:",
                "              keyword: \"Stream a log\"",
                "").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(className));
            keywordClass.transferTo(zip);
            zip.closeEntry();
        }
        return automationPackage;
    }

    /**
     * Packs {@code src/test/resources/streaming-node-automation-package} into an automation package, the same way as
     * {@link NodeLocalExecutionTest}
     */
    private File buildNodeAutomationPackage() throws IOException {
        Path sources = Path.of("src/test/resources/streaming-node-automation-package");
        Path keywordLibrary = workDirectory.getRoot().toPath().resolve(NODE_KEYWORD_LIBRARY_ARCHIVE);
        try (OutputStream out = Files.newOutputStream(keywordLibrary);
             ZipOutputStream zip = new ZipOutputStream(out);
             Stream<Path> files = Files.walk(sources.resolve("keyword-library"))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                addEntry(zip, file, sources.resolve("keyword-library").relativize(file).toString().replace('\\', '/'));
            }
        }
        File automationPackage = workDirectory.newFile("node-streaming-automation-package.zip");
        try (OutputStream out = Files.newOutputStream(automationPackage.toPath());
             ZipOutputStream zip = new ZipOutputStream(out)) {
            addEntry(zip, sources.resolve("automation-package.yml"), "automation-package.yml");
            addEntry(zip, keywordLibrary, NODE_KEYWORD_LIBRARY_ARCHIVE);
        }
        return automationPackage;
    }

    private static void addEntry(ZipOutputStream zip, Path file, String name) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        Files.copy(file, zip);
        zip.closeEntry();
    }
}
