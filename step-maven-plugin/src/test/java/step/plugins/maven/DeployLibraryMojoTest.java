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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import step.cli.DeployLibraryTool;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Set;

public class DeployLibraryMojoTest extends AbstractMojoTest {

    @Test
    public void testUpload() throws Exception {
        DeployLibraryMojoTestable mojo = new DeployLibraryMojoTestable();

        // configure mojo with test parameters and mocked Maven Project
        configureMojo(mojo);
        mojo.execute();

        Mockito.verify(mojo.mockedTool, Mockito.times(1)).execute();
        Assert.assertEquals("http://localhost:8080", mojo.toolUrl);
        Assert.assertEquals(TENANT_1.getName(), mojo.toolProjectName);
        Assert.assertEquals("dummyToken", mojo.toolAuthToken);
    }

    private void configureMojo(DeployLibraryMojoTestable mojo) throws URISyntaxException {
        mojo.setUrl("http://localhost:8080");
        mojo.setBuildFinalName("Test build name");
        mojo.setProjectVersion(VERSION_ID);
        mojo.setArtifactClassifier("jar-with-dependencies");
        mojo.setStepProjectName(TENANT_1.getName());
        mojo.setAuthToken("dummyToken");

        MavenProject mockedProject = Mockito.mock(MavenProject.class);
        Artifact mainArtifact = createArtifactMock();

        Mockito.when(mockedProject.getArtifact()).thenReturn(mainArtifact);
        Mockito.when(mockedProject.getArtifactId()).thenReturn(ARTIFACT_ID);
        Mockito.when(mockedProject.getGroupId()).thenReturn(GROUP_ID);
        Mockito.when(mockedProject.getVersion()).thenReturn(VERSION_ID);

        Artifact jarWithDependenciesArtifact = createArtifactWithDependenciesMock();
        Mockito.when(mockedProject.getArtifacts()).thenReturn(Set.of(mainArtifact, jarWithDependenciesArtifact));
        Mockito.when(mockedProject.getAttachedArtifacts()).thenReturn(Arrays.asList(mainArtifact, jarWithDependenciesArtifact));
        mojo.setProject(mockedProject);
    }

    private static class DeployLibraryMojoTestable extends DeployLibraryMojo {

        private final DeployLibraryTool mockedTool = Mockito.mock(DeployLibraryTool.class);

        private String toolUrl;
        private String toolProjectName;
        private String toolAuthToken;


        public DeployLibraryMojoTestable() {
        }

        @Override
        protected DeployLibraryTool createTool(String url, String projectName, String authToken, String managedLibraryName) {
            this.toolUrl = url;
            this.toolProjectName = projectName;
            this.toolAuthToken = authToken;
            return mockedTool;
        }

        @Override
        protected void checkStepControllerVersion() throws MojoExecutionException {
            //mock the check
        }
    }
}