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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import step.cli.local.LocalAgentProvisioningConfiguration;
import step.core.Constants;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Runs an automation package with a Groovy and a JavaScript keyword.
 * <p>
 * Both run through {@code javax.script} on the agent, whose class path holds neither engine. The CLI has to send them
 * along with the keyword, the way a controller sends the content of its {@code ext/groovy} directory, and this test
 * is what proves the engines it extracts from its own jar are complete enough to load and run a script.
 */
public class ScriptKeywordLocalExecutionTest {

    @Rule
    public final TemporaryFolder workDirectory = new TemporaryFolder();

    @Test
    public void executesGroovyAndJavascriptKeywordsOnALocalAgent() throws Exception {
        LocalAgentProvisioningConfiguration configuration = new LocalAgentProvisioningConfiguration()
            .setWorkDirectory(workDirectory.getRoot().toPath());

        File automationPackage = workDirectory.newFile("script-ap.zip");
        zip(Path.of("src/test/resources/script-ap"), automationPackage.toPath());

        // Fails the test if any plan of the package fails, the handler reports plan failures by throwing
        new ApLocalExecuteCommandHandler()
            .execute(automationPackage, null, null, null, null, null, Map.of(), configuration);

        for (Entry<String, String> engine : Map.of("groovy", "groovy/lang/GroovyShell.class",
            "javascript", "org/openjdk/nashorn/api/scripting/NashornScriptEngineFactory.class").entrySet()) {
            Path directory = workDirectory.getRoot().toPath().resolve("libraries")
                .resolve(engine.getKey()).resolve(Constants.STEP_VERSION_STRING);
            Assert.assertTrue("The " + engine.getKey() + " libraries should have been extracted to " + directory,
                Files.isDirectory(directory));
        }
    }

    private static void zip(Path directory, Path archive) throws IOException {
        try (OutputStream out = Files.newOutputStream(archive);
             ZipOutputStream zip = new ZipOutputStream(out);
             Stream<Path> entries = Files.walk(directory)) {
            for (Path entry : entries.filter(Files::isRegularFile).toList()) {
                zip.putNextEntry(new ZipEntry(directory.relativize(entry).toString().replace('\\', '/')));
                Files.copy(entry, zip);
                zip.closeEntry();
            }
        }
    }
}
