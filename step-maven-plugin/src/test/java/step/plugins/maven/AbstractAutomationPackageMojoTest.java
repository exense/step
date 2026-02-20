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
package step.plugins.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;

public class AbstractAutomationPackageMojoTest {

    // --- prepareLibrary tests ---

    @Test
    public void prepareLibrary_withNoConfig_returnsEmptyConfig() throws MojoExecutionException {
        TestableAutomationPackageMojo mojo = new TestableAutomationPackageMojo();

        LibraryConfiguration result = mojo.prepareLibrary();

        Assert.assertFalse(result.isSet());
    }

    @Test
    public void prepareLibrary_withXmlMavenCoordinatesOnly_returnsSameConfig() throws MojoExecutionException {
        TestableAutomationPackageMojo mojo = new TestableAutomationPackageMojo();
        LibraryConfiguration lib = new LibraryConfiguration();
        lib.setGroupId("com.example");
        lib.setArtifactId("my-lib");
        lib.setVersion("1.0.0");
        lib.setClassifier("sources");
        lib.setType("jar");
        mojo.library = lib;

        LibraryConfiguration result = mojo.prepareLibrary();

        Assert.assertEquals("com.example", result.getGroupId());
        Assert.assertEquals("my-lib", result.getArtifactId());
        Assert.assertEquals("1.0.0", result.getVersion());
        Assert.assertEquals("sources", result.getClassifier());
        Assert.assertEquals("jar", result.getType());
        Assert.assertNull(result.getPath());
        Assert.assertNull(result.getManaged());
        Assert.assertTrue(result.isMavenCoordinatesConfigured());
    }

    @Test
    public void prepareLibrary_withXmlPathOnly_returnsSameConfig() throws MojoExecutionException {
        TestableAutomationPackageMojo mojo = new TestableAutomationPackageMojo();
        LibraryConfiguration lib = new LibraryConfiguration();
        lib.setPath("/opt/libs/my-lib.jar");
        mojo.library = lib;

        LibraryConfiguration result = mojo.prepareLibrary();

        Assert.assertEquals("/opt/libs/my-lib.jar", result.getPath());
        Assert.assertTrue(result.isPathConfigured());
    }

    @Test
    public void prepareLibrary_withXmlManagedOnly_returnsSameConfig() throws MojoExecutionException {
        TestableAutomationPackageMojo mojo = new TestableAutomationPackageMojo();
        LibraryConfiguration lib = new LibraryConfiguration();
        lib.setManaged("my-managed-lib");
        mojo.library = lib;

        LibraryConfiguration result = mojo.prepareLibrary();

        Assert.assertEquals("my-managed-lib", result.getManaged());
        Assert.assertTrue(result.isManagedLibraryNameConfigured());
    }

    @Test
    public void prepareLibrary_withFlatPropsMavenCoordinates_appliesCoordinates() throws MojoExecutionException {
        TestableAutomationPackageMojo mojo = new TestableAutomationPackageMojo();
        mojo.libraryGroupId = "com.example";
        mojo.libraryArtifactId = "flat-lib";
        mojo.libraryVersion = "2.0.0";
        mojo.libraryClassifier = "tests";
        mojo.libraryType = "jar";

        LibraryConfiguration result = mojo.prepareLibrary();

        Assert.assertEquals("com.example", result.getGroupId());
        Assert.assertEquals("flat-lib", result.getArtifactId());
        Assert.assertEquals("2.0.0", result.getVersion());
        Assert.assertEquals("tests", result.getClassifier());
        Assert.assertEquals("jar", result.getType());
        Assert.assertTrue(result.isMavenCoordinatesConfigured());
    }

    @Test
    public void prepareLibrary_withFlatPropsPath_appliesPath() throws MojoExecutionException {
        TestableAutomationPackageMojo mojo = new TestableAutomationPackageMojo();
        mojo.libraryPath = "/tmp/my-lib.jar";

        LibraryConfiguration result = mojo.prepareLibrary();

        Assert.assertEquals("/tmp/my-lib.jar", result.getPath());
        Assert.assertTrue(result.isPathConfigured());
    }

    @Test
    public void prepareLibrary_withFlatPropsManaged_appliesManaged() throws MojoExecutionException {
        TestableAutomationPackageMojo mojo = new TestableAutomationPackageMojo();
        mojo.libraryManaged = "managed-lib-name";

        LibraryConfiguration result = mojo.prepareLibrary();

        Assert.assertEquals("managed-lib-name", result.getManaged());
        Assert.assertTrue(result.isManagedLibraryNameConfigured());
    }

    @Test
    public void prepareLibrary_flatPropsOverrideXmlCoordinates() throws MojoExecutionException {
        TestableAutomationPackageMojo mojo = new TestableAutomationPackageMojo();
        // XML config sets all coordinates
        LibraryConfiguration lib = new LibraryConfiguration();
        lib.setGroupId("xml.group");
        lib.setArtifactId("xml-lib");
        lib.setVersion("1.0.0");
        mojo.library = lib;
        // Flat property overrides only groupId and version
        mojo.libraryGroupId = "override.group";
        mojo.libraryVersion = "9.9.9";

        LibraryConfiguration result = mojo.prepareLibrary();

        Assert.assertEquals("override.group", result.getGroupId());   // overridden
        Assert.assertEquals("xml-lib", result.getArtifactId());        // kept from XML
        Assert.assertEquals("9.9.9", result.getVersion());             // overridden
    }

    // --- Minimal concrete subclass for testing ---

    private static class TestableAutomationPackageMojo extends AbstractAutomationPackageMojo {

        private LibraryConfiguration library;
        String libraryGroupId;
        String libraryArtifactId;
        String libraryVersion;
        String libraryClassifier;
        String libraryType;
        String libraryPath;
        String libraryManaged;

        @Override
        public LibraryConfiguration getLibrary() { return library; }

        @Override
        public String getLibraryGroupId() { return libraryGroupId; }

        @Override
        public String getLibraryArtifactId() { return libraryArtifactId; }

        @Override
        public String getLibraryVersion() { return libraryVersion; }

        @Override
        public String getLibraryClassifier() { return libraryClassifier; }

        @Override
        public String getLibraryType() { return libraryType; }

        @Override
        public String getLibraryPath() { return libraryPath; }

        @Override
        public String getLibraryManaged() { return libraryManaged; }

        @Override
        public void execute() throws MojoExecutionException { }
    }
}
