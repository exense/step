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

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import step.client.executions.RemoteExecutionFuture;
import step.client.executions.RemoteExecutionManager;
import step.client.resources.RemoteResourceManager;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.repositories.ImportResult;
import step.resources.Resource;
import step.resources.SimilarResourceExistingException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class RunPackagedAutomationPackagesMojoOSTest extends AbstractMojoTest {

	@Test
	public void testExecuteOk() throws InterruptedException, TimeoutException, MojoExecutionException, MojoFailureException, URISyntaxException, SimilarResourceExistingException, IOException {
		RemoteExecutionManager remoteExecutionManagerMock = createExecutionManagerMock(ReportNodeStatus.PASSED);

		RemoteResourceManager remoteResourceManagerMock = Mockito.mock(RemoteResourceManager.class);
		Resource resourceMock = Mockito.mock(Resource.class);
		Mockito.when(resourceMock.getId()).thenReturn(new ObjectId());

		Mockito.when(remoteResourceManagerMock.createResource(Mockito.anyString(), Mockito.any(InputStream.class), Mockito.anyString(), Mockito.anyBoolean(), Mockito.isNull())).thenReturn(resourceMock);

		RunPackagedAutomationPackagesMojoOSTestable mojo = new RunPackagedAutomationPackagesMojoOSTestable(remoteExecutionManagerMock, remoteResourceManagerMock);
		configureMojo(mojo);
		mojo.execute();

		ArgumentCaptor<InputStream> fileCaptor = ArgumentCaptor.forClass(InputStream.class);
		Mockito.verify(remoteResourceManagerMock, Mockito.times(1)).createResource(Mockito.anyString(), fileCaptor.capture(), Mockito.anyString(), Mockito.eq(false), Mockito.isNull());
		FileInputStream capturedFile = (FileInputStream) fileCaptor.getValue();
		Assert.assertEquals(Arrays.asList("jar-with-dependencies content"), IOUtils.readLines(capturedFile, StandardCharsets.UTF_8));

		ArgumentCaptor<ExecutionParameters> captor = ArgumentCaptor.forClass(ExecutionParameters.class);
		Mockito.verify(remoteExecutionManagerMock, Mockito.times(1)).execute(captor.capture());
		ExecutionParameters captured = captor.getValue();
		Assert.assertEquals("Test description", captured.getDescription());
		Assert.assertNull(captured.getUserID());
		Assert.assertEquals("ResourceArtifact", captured.getRepositoryObject().getRepositoryID());
		Assert.assertEquals(ExecutionMode.RUN, captured.getMode());
		Assert.assertEquals(resourceMock.getId().toString(), captured.getRepositoryObject().getRepositoryParameters().get("resourceId"));
		Assert.assertEquals(createTestCustomParams(), captured.getCustomParameters());
	}

	private RemoteExecutionManager createExecutionManagerMock(ReportNodeStatus resultStatus) throws TimeoutException, InterruptedException {
		RemoteExecutionManager remoteExecutionManagerMock = Mockito.mock(RemoteExecutionManager.class);
		Mockito.when(remoteExecutionManagerMock.execute(Mockito.any(ExecutionParameters.class))).thenReturn("exec-id-1");

		RemoteExecutionFuture remoteExecutionFuture = Mockito.mock(RemoteExecutionFuture.class);
		Mockito.when(remoteExecutionFuture.waitForExecutionToTerminate(Mockito.anyLong())).thenReturn(remoteExecutionFuture);

		Execution execution = Mockito.mock(Execution.class);
		Mockito.when(remoteExecutionFuture.getExecution()).thenReturn(execution);
		Mockito.when(execution.getStatus()).thenReturn(ExecutionStatus.ENDED);
		Mockito.when(execution.getResult()).thenReturn(resultStatus);
		ImportResult t = new ImportResult();
		t.setErrors(new ArrayList<>());
		if(resultStatus.equals(ReportNodeStatus.PASSED)){
			t.setSuccessful(true);
		}
		Mockito.when(execution.getImportResult()).thenReturn(t);

		Mockito.when(remoteExecutionManagerMock.getFuture("exec-id-1")).thenReturn(remoteExecutionFuture);
		return remoteExecutionManagerMock;
	}

	private void configureMojo(RunPackagedAutomationPackagesMojoOSTestable mojo) throws URISyntaxException {
		mojo.setArtifactId(ARTIFACT_ID);
		mojo.setArtifactClassifier("jar-with-dependencies");
		mojo.setArtifactVersion(VERSION_ID);
		mojo.setGroupId(GROUP_ID);
		mojo.setDescription("Test description");
		mojo.setUrl("http://localhost:8080");
		mojo.setBuildFinalName("Test build name");
		mojo.setProjectVersion("1.0.0");
		mojo.setExecutionResultTimeoutS(3);
		mojo.setWaitForExecution(true);
		mojo.setEnsureExecutionSuccess(true);

		Map<String, String> params = createTestCustomParams();
		mojo.setExecutionParameters(params);

		MavenProject mockedProject = Mockito.mock(MavenProject.class);

		Artifact mainArtifact = createArtifactMock();

		Mockito.when(mockedProject.getArtifact()).thenReturn(mainArtifact);

		Mockito.when(mockedProject.getArtifacts()).thenReturn(new HashSet<>());

		Artifact jarWithDependenciesArtifact = createArtifactWithDependenciesMock();

		Mockito.when(mockedProject.getAttachedArtifacts()).thenReturn(Arrays.asList(jarWithDependenciesArtifact));

		mojo.setProject(mockedProject);
	}

	private static Map<String, String> createTestCustomParams() {
		Map<String, String> params = new HashMap<>();
		params.put("param1", "value1");
		params.put("param2", "value2");
		return params;
	}

	private static class RunPackagedAutomationPackagesMojoOSTestable extends RunPackagedAutomationPackagesMojoOS {

		private RemoteExecutionManager remoteExecutionManagerMock;
		private RemoteResourceManager remoteResourceManagerMock;

		public RunPackagedAutomationPackagesMojoOSTestable(RemoteExecutionManager remoteExecutionManagerMock, RemoteResourceManager remoteResourceManagerMock) {
			super();
			this.remoteExecutionManagerMock = remoteExecutionManagerMock;
			this.remoteResourceManagerMock = remoteResourceManagerMock;
		}

		@Override
		protected RemoteExecutionManager createRemoteExecutionManager() {
			return remoteExecutionManagerMock;
		}

		@Override
		protected RemoteResourceManager createRemoteResourceManager() {
			return remoteResourceManagerMock;
		}
	}
}