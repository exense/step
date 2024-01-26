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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import step.automation.packages.client.AutomationPackageClientException;
import step.automation.packages.client.RemoteAutomationPackageClientImpl;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;

public class DeployAutomationPackageMojoTest extends AbstractMojoTest {

    private static final String UPDATED_PACK_ID = "updatedPackId";

    @Test
    public void testUpload() throws Exception {
        RemoteAutomationPackageClientImpl automationPackageClient = createRemoteAutomationPackageClientMock();
        DeployAutomationPackageMojoTestable mojo = new DeployAutomationPackageMojoTestable(automationPackageClient);

        // configure mojo with test parameters and mocked Maven Project
        configureMojo(mojo);
        mojo.execute();


        // attributes used to search for existing function packages
        ArgumentCaptor<File> packageFileCaptor = ArgumentCaptor.forClass(File.class);
        Mockito.verify(automationPackageClient, Mockito.times(1)).createOrUpdateAutomationPackage(packageFileCaptor.capture());
        Mockito.verify(automationPackageClient, Mockito.times(1)).close();
        Mockito.verifyNoMoreInteractions(automationPackageClient);
        Assert.assertEquals(mojo.getProject().getAttachedArtifacts().get(0).getFile(), packageFileCaptor.getValue());
    }

    private void configureMojo(DeployAutomationPackageMojoTestable mojo) throws URISyntaxException {
        mojo.setUrl("http://localhost:8080");
        mojo.setBuildFinalName("Test build name");
        mojo.setProjectVersion("1.0.0");
        mojo.setArtifactId(ARTIFACT_ID);
        mojo.setArtifactVersion(VERSION_ID);
        mojo.setGroupId(GROUP_ID);
        mojo.setArtifactClassifier("jar-with-dependencies");

        MavenProject mockedProject = Mockito.mock(MavenProject.class);
        Artifact mainArtifact = createArtifactMock();

        Mockito.when(mockedProject.getArtifact()).thenReturn(mainArtifact);
        Mockito.when(mockedProject.getArtifacts()).thenReturn(new HashSet<>());
        Mockito.when(mockedProject.getArtifactId()).thenReturn(ARTIFACT_ID);
        Mockito.when(mockedProject.getGroupId()).thenReturn(GROUP_ID);

        Artifact jarWithDependenciesArtifact = createArtifactWithDependenciesMock();
        Mockito.when(mockedProject.getAttachedArtifacts()).thenReturn(Arrays.asList(jarWithDependenciesArtifact));
        mojo.setProject(mockedProject);
    }


    private RemoteAutomationPackageClientImpl createRemoteAutomationPackageClientMock() throws AutomationPackageClientException {
        RemoteAutomationPackageClientImpl remoteClient = Mockito.mock(RemoteAutomationPackageClientImpl.class);
        Mockito.when(remoteClient.createOrUpdateAutomationPackage(Mockito.any())).thenReturn(UPDATED_PACK_ID);
        return remoteClient;
    }

    private static class DeployAutomationPackageMojoTestable extends DeployAutomationPackageMojo {

        private RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock;

        public DeployAutomationPackageMojoTestable(RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock) {
            this.remoteAutomationPackageClientMock = remoteAutomationPackageClientMock;
        }

        protected RemoteAutomationPackageClientImpl createRemoteAutomationPackageClient() {
            return remoteAutomationPackageClientMock;
        }

    }
}