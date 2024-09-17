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
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import step.cli.AbstractDeployAutomationPackageTool;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Set;

public class DeployAutomationPackageMojoTest extends AbstractMojoTest {

    @Test
    public void testUpload() throws Exception {
        DeployAutomationPackageMojoTestable mojo = new DeployAutomationPackageMojoTestable();

        // configure mojo with test parameters and mocked Maven Project
        configureMojo(mojo);
        mojo.execute();

        Mockito.verify(mojo.mockedTool, Mockito.times(1)).execute();
        Assert.assertEquals("http://localhost:8080", mojo.toolUrl);
        Assert.assertEquals(false, mojo.toolAsync);
        Assert.assertEquals(TENANT_1.getName(), mojo.toolProjectName);
        Assert.assertNull(mojo.toolAuthToken);
    }

    private void configureMojo(DeployAutomationPackageMojoTestable mojo) throws URISyntaxException {
        mojo.setUrl("http://localhost:8080");
        mojo.setBuildFinalName("Test build name");
        mojo.setProjectVersion(VERSION_ID);
        mojo.setArtifactClassifier("jar-with-dependencies");
        mojo.setStepProjectName(TENANT_1.getName());
        mojo.setAsync(false);

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

    private static class DeployAutomationPackageMojoTestable extends DeployAutomationPackageMojo {

        private final AbstractDeployAutomationPackageTool mockedTool = Mockito.mock(AbstractDeployAutomationPackageTool.class);

        private String toolUrl;
        private String toolProjectName;
        private String toolAuthToken;
        private Boolean toolAsync;

        public DeployAutomationPackageMojoTestable() {
        }

        @Override
        protected AbstractDeployAutomationPackageTool createTool(String url, String projectName, String authToken, Boolean async) {
            this.toolAsync = async;
            this.toolUrl = url;
            this.toolProjectName = projectName;
            this.toolAuthToken = authToken;
            return mockedTool;
        }
    }
}