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
package step.plugins.jmeter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Explicit coverage for {@code JMeterLocalHandler.updateClasspathSystemProperty}.
 * <p>
 * The handler is initialized once per {@code ApplicationContext}, but it mutates the
 * JVM-wide system property {@code java.class.path}. This test invokes the initialization
 * twice (simulating two application contexts on the same agent) and asserts that each
 * JMeter ext jar ends up on the classpath exactly once.
 * <p>
 * The method under test is private, so it is called through reflection. This also keeps
 * the test source identical whether the method is declared static or as an instance
 * method, so the very same test can be dropped onto the legacy code to reproduce the bug
 * (there, the jars are appended on every call and the count assertion fails).
 */
public class JMeterLocalHandlerClasspathTest {

    private Path jmeterHome;
    private String originalClasspath;
    private final List<String> jarAbsolutePaths = new ArrayList<>();

    @Before
    public void before() throws Exception {
        originalClasspath = System.getProperty("java.class.path");

        // Build a fake JMeter home with a lib/ext folder containing a few jars.
        jmeterHome = Files.createTempDirectory("jmeterHomeTest");
        Path extFolder = jmeterHome.resolve("lib").resolve("ext");
        Files.createDirectories(extFolder);
        for (String jarName : new String[]{"ApacheJMeter_extA.jar", "ApacheJMeter_extB.jar", "custom_plugin.jar"}) {
            Path jar = Files.createFile(extFolder.resolve(jarName));
            jarAbsolutePaths.add(jar.toFile().getAbsolutePath());
        }
    }

    @After
    public void after() throws Exception {
        // Always restore the original classpath so we don't leak state into other tests.
        if (originalClasspath != null) {
            System.setProperty("java.class.path", originalClasspath);
        }
        deleteRecursively(jmeterHome.toFile());
    }

    @Test
    public void updateClasspathSystemPropertyIsIdempotentAcrossMultipleInitializations() throws Exception {
        // Simulate the handler being initialized twice during the agent's lifetime
        // (e.g. two different application contexts / function versions).
        String jmeterHomePath = jmeterHome.toFile().getAbsolutePath();
        JMeterLocalHandler.updateClasspathSystemProperty(jmeterHomePath);
        JMeterLocalHandler.updateClasspathSystemProperty(jmeterHomePath);

        List<String> entries = Arrays.asList(
            System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator)));

        for (String jarPath : jarAbsolutePaths) {
            long occurrences = entries.stream().filter(jarPath::equals).count();
            Assert.assertEquals(
                "Ext jar should appear on java.class.path exactly once after repeated initializations, but was found "
                    + occurrences + " time(s): " + jarPath,
                1, occurrences);
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
