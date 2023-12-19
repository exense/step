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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import step.automation.packages.client.RemoteAutomationPackageClientImpl;
import step.automation.packages.execution.AutomationPackageExecutionParameters;
import step.client.executions.RemoteExecutionManager;
import step.controller.multitenancy.client.MultitenancyClient;
import step.controller.multitenancy.client.RemoteMultitenancyClientImpl;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.repositories.ImportResult;

import java.io.File;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class ExecuteAutomationPackageMojoTest extends AbstractMojoTest {

	@Test
	public void testExecuteOk() throws InterruptedException, TimeoutException, MojoExecutionException, MojoFailureException, URISyntaxException {
		RemoteExecutionManager remoteExecutionManagerMock = createExecutionManagerMock(ReportNodeStatus.PASSED);

		RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock = Mockito.mock(RemoteAutomationPackageClientImpl.class);
		RemoteMultitenancyClientImpl multitenancyClientMock = createRemoteMultitenancyClientMock();

		RunAutomationPackageMojoTestable mojo = new RunAutomationPackageMojoTestable(remoteExecutionManagerMock, remoteAutomationPackageClientMock, multitenancyClientMock);
		configureMojo(mojo);
		mojo.execute();

		ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
		ArgumentCaptor<AutomationPackageExecutionParameters> executionParamsCaptor = ArgumentCaptor.forClass(AutomationPackageExecutionParameters.class);
		Mockito.verify(remoteAutomationPackageClientMock, Mockito.times(1)).executeAutomationPackage(fileCaptor.capture(), executionParamsCaptor.capture());
		File capturedFile = fileCaptor.getValue();
		Assert.assertEquals("test-file-jar.jar", capturedFile.getName());

		ArgumentCaptor<ExecutionParameters> captor = ArgumentCaptor.forClass(ExecutionParameters.class);
		Mockito.verify(remoteExecutionManagerMock, Mockito.times(1)).execute(captor.capture());
		ExecutionParameters captured = captor.getValue();
		Assert.assertEquals("Test description", captured.getDescription());
		Assert.assertEquals("testUser", captured.getUserID());
		Assert.assertEquals("ResourceArtifact", captured.getRepositoryObject().getRepositoryID());
		Assert.assertEquals(ExecutionMode.RUN, captured.getMode());

		Assert.assertEquals(createTestCustomParams(), captured.getCustomParameters());
	}

	private RemoteExecutionManager createExecutionManagerMock(ReportNodeStatus resultStatus) throws TimeoutException, InterruptedException {
		RemoteExecutionManager remoteExecutionManagerMock = Mockito.mock(RemoteExecutionManager.class);

		Execution execution = Mockito.mock(Execution.class);
		Mockito.when(execution.getStatus()).thenReturn(ExecutionStatus.ENDED);
		Mockito.when(execution.getResult()).thenReturn(resultStatus);
		ImportResult t = new ImportResult();
		t.setErrors(new ArrayList<>());
		if(resultStatus.equals(ReportNodeStatus.PASSED)){
			t.setSuccessful(true);
		}
		Mockito.when(execution.getImportResult()).thenReturn(t);

		Mockito.when(remoteExecutionManagerMock.waitForTermination(Mockito.anyList(), Mockito.anyLong())).thenReturn(List.of(execution));
		return remoteExecutionManagerMock;
	}

	private void configureMojo(RunAutomationPackageMojoTestable mojo) throws URISyntaxException {
		mojo.setArtifactId(ARTIFACT_ID);
		mojo.setArtifactClassifier("jar-with-dependencies");
		mojo.setArtifactVersion(VERSION_ID);
		mojo.setGroupId(GROUP_ID);
		mojo.setUrl("http://localhost:8080");
		mojo.setAuthToken("abc");
		mojo.setBuildFinalName("Test build name");
		mojo.setProjectVersion("1.0.0");
		mojo.setExecutionResultTimeoutS(3);
		mojo.setUserId("testUser");
		Map<String, String> params = createTestCustomParams();
		mojo.setExecutionParameters(params);
		mojo.setWaitForExecution(true);
		mojo.setEnsureExecutionSuccess(true);

		mojo.setIncludePlans(TEST_INCLUDE_PLANS);

		MavenProject mockedProject = Mockito.mock(MavenProject.class);

		Artifact mainArtifact = createArtifactMock();

		Mockito.when(mockedProject.getArtifact()).thenReturn(mainArtifact);

		Mockito.when(mockedProject.getArtifacts()).thenReturn(new HashSet<>());

		Artifact jarWithDependenciesArtifact = createArtifactWithDependenciesMock();

		Mockito.when(mockedProject.getAttachedArtifacts()).thenReturn(Arrays.asList(jarWithDependenciesArtifact));

		mojo.setProject(mockedProject);

		mojo.setStepProjectName(TENANT_1.getName());
	}

	private static Map<String, String> createTestCustomParams() {
		Map<String, String> params = new HashMap<>();
		params.put("param1", "value1");
		params.put("param2", "value2");
		return params;
	}

	private RemoteMultitenancyClientImpl createRemoteMultitenancyClientMock(){
		RemoteMultitenancyClientImpl mock = Mockito.mock(RemoteMultitenancyClientImpl.class);
		Mockito.when(mock.getAvailableTenants()).thenReturn(List.of(TENANT_1, TENANT_2));
		return mock;
	}

	private static class RunAutomationPackageMojoTestable extends ExecuteAutomationPackageMojo {

		private final RemoteExecutionManager remoteExecutionManagerMock;
		private final RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock;
		private final MultitenancyClient multitenancyClientMock;

		public RunAutomationPackageMojoTestable(RemoteExecutionManager remoteExecutionManagerMock, RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock, MultitenancyClient multitenancyClientMock) {
			super();
			this.remoteExecutionManagerMock = remoteExecutionManagerMock;
			this.remoteAutomationPackageClientMock = remoteAutomationPackageClientMock;
			this.multitenancyClientMock = multitenancyClientMock;
		}

		@Override
		protected RemoteExecutionManager createRemoteExecutionManager() {
			return remoteExecutionManagerMock;
		}

		@Override
		protected RemoteAutomationPackageClientImpl createRemoteAutomationPackageClient() {
			return remoteAutomationPackageClientMock;
		}

		@Override
		protected MultitenancyClient createMultitenancyClient() {
			return multitenancyClientMock;
		}
	}
}