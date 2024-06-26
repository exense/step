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
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import step.automation.packages.client.AutomationPackageClientException;
import step.automation.packages.client.RemoteAutomationPackageClientImpl;
import step.client.executions.RemoteExecutionFuture;
import step.client.executions.RemoteExecutionManager;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.AutomationPackageExecutionParameters;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionStatus;
import step.core.plans.filters.PlanByIncludedNamesFilter;
import step.core.repositories.ImportResult;

import java.io.File;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ExecuteAutomationPackageMojoTest extends AbstractMojoTest {

	@Test
	public void testExecuteOk() throws Exception {
		Execution execution = getMockedExecution(ReportNodeStatus.PASSED);

		List<Execution> executions = List.of(execution);
		List<String> executionIds = getExecuteAutomationPackageResult(executions);

		RemoteExecutionManager remoteExecutionManagerMock = createExecutionManagerMock(executions);

		RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock = Mockito.mock(RemoteAutomationPackageClientImpl.class);
		Mockito.when(remoteAutomationPackageClientMock.executeAutomationPackage(Mockito.any(), Mockito.any())).thenReturn(executionIds);

		RunAutomationPackageMojoTestable mojo = new RunAutomationPackageMojoTestable(remoteExecutionManagerMock, remoteAutomationPackageClientMock);
		configureMojo(mojo, true);
		mojo.execute();

		assertAutomationPackageClientMockCalls(remoteAutomationPackageClientMock);
	}

	private static List<String> getExecuteAutomationPackageResult(List<Execution> executions) {
		return executions.stream().map(e -> e.getId().toString()).collect(Collectors.toList());
	}

	@Test
	public void testExecuteImportError() throws Exception {
		Execution execution = getMockedExecution(ReportNodeStatus.FAILED, "Import error");

		List<Execution> executions = List.of(execution);
		List<String> executionIds = getExecuteAutomationPackageResult(executions);

		RemoteExecutionManager remoteExecutionManagerMock = createExecutionManagerMock(executions);

		RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock = Mockito.mock(RemoteAutomationPackageClientImpl.class);
		Mockito.when(remoteAutomationPackageClientMock.executeAutomationPackage(Mockito.any(), Mockito.any())).thenReturn(executionIds);

		RunAutomationPackageMojoTestable mojo = new RunAutomationPackageMojoTestable(remoteExecutionManagerMock, remoteAutomationPackageClientMock);
		configureMojo(mojo, true);
		Assert.assertThrows(MojoExecutionException.class, mojo::execute);
		assertAutomationPackageClientMockCalls(remoteAutomationPackageClientMock);
	}

	@Test
	public void testExecuteNotOk() throws Exception {
		Execution execution = getMockedExecution(ReportNodeStatus.FAILED);

		List<Execution> executions = List.of(execution);
		List<String> executionIds = getExecuteAutomationPackageResult(executions);

		RemoteExecutionManager remoteExecutionManagerMock = createExecutionManagerMock(executions);

		RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock = Mockito.mock(RemoteAutomationPackageClientImpl.class);
		Mockito.when(remoteAutomationPackageClientMock.executeAutomationPackage(Mockito.any(), Mockito.any())).thenReturn(executionIds);

		RunAutomationPackageMojoTestable mojo = new RunAutomationPackageMojoTestable(remoteExecutionManagerMock, remoteAutomationPackageClientMock);
		configureMojo(mojo, true);
		Assert.assertThrows(MojoExecutionException.class, mojo::execute);
		assertAutomationPackageClientMockCalls(remoteAutomationPackageClientMock);
	}

	@Test
	public void testExecuteNotOkWithout() throws Exception {
		Execution execution = getMockedExecution(ReportNodeStatus.FAILED);

		List<Execution> executions = List.of(execution);
		List<String> executionIds = getExecuteAutomationPackageResult(executions);

		RemoteExecutionManager remoteExecutionManagerMock = createExecutionManagerMock(executions);

		RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock = Mockito.mock(RemoteAutomationPackageClientImpl.class);
		Mockito.when(remoteAutomationPackageClientMock.executeAutomationPackage(Mockito.any(), Mockito.any())).thenReturn(executionIds);

		RunAutomationPackageMojoTestable mojo = new RunAutomationPackageMojoTestable(remoteExecutionManagerMock, remoteAutomationPackageClientMock);

		configureMojo(mojo, false);
		mojo.execute();
		assertAutomationPackageClientMockCalls(remoteAutomationPackageClientMock);
	}

	private static void assertAutomationPackageClientMockCalls(RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock) throws AutomationPackageClientException {
		ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
		ArgumentCaptor<AutomationPackageExecutionParameters> executionParamsCaptor = ArgumentCaptor.forClass(AutomationPackageExecutionParameters.class);
		Mockito.verify(remoteAutomationPackageClientMock, Mockito.times(1)).executeAutomationPackage(fileCaptor.capture(), executionParamsCaptor.capture());
		File capturedFile = fileCaptor.getValue();
		Assert.assertEquals("test-file-jar-with-dependencies.jar", capturedFile.getName());

		AutomationPackageExecutionParameters captured = executionParamsCaptor.getValue();
		Assert.assertEquals("testUser", captured.getUserID());
		Assert.assertEquals(ExecutionMode.RUN, captured.getMode());
		Assert.assertEquals(List.of("plan1", "plan2"), ((PlanByIncludedNamesFilter) captured.getPlanFilter()).getIncludedNames());

		Assert.assertEquals(createTestCustomParams(), captured.getCustomParameters());
	}

	private static Execution getMockedExecution(ReportNodeStatus resultStatus) {
		return getMockedExecution(resultStatus, null);
	}

	private static Execution getMockedExecution(ReportNodeStatus resultStatus, String importError) {
		Execution execution = Mockito.mock(Execution.class);
		Mockito.when(execution.getId()).thenReturn(new ObjectId());
		Mockito.when(execution.getDescription()).thenReturn("My execution");
		Mockito.when(execution.getStatus()).thenReturn(ExecutionStatus.ENDED);
		Mockito.when(execution.getResult()).thenReturn(resultStatus);

		ImportResult t = new ImportResult();
		if(importError != null) {
			t.setErrors(List.of(importError));
			t.setSuccessful(false);
		} else {
			t.setSuccessful(true);
		}
		Mockito.when(execution.getImportResult()).thenReturn(t);
		return execution;
	}

	private RemoteExecutionManager createExecutionManagerMock(List<Execution> executions) throws TimeoutException, InterruptedException {
		RemoteExecutionManager remoteExecutionManagerMock = Mockito.mock(RemoteExecutionManager.class);
		Mockito.when(remoteExecutionManagerMock.get(Mockito.any())).thenAnswer(invocationOnMock -> executions.stream().filter(e -> e.getId().toString().equals(invocationOnMock.getArgument(0))).findFirst().get());
		Mockito.when(remoteExecutionManagerMock.waitForTermination(Mockito.anyList(), Mockito.anyLong())).thenReturn(executions);
		RemoteExecutionFuture futureMock = Mockito.mock(RemoteExecutionFuture.class);
		Mockito.when(futureMock.getErrorSummary()).thenReturn("Error summary...");
		Mockito.when((remoteExecutionManagerMock.getFuture(Mockito.anyString()))).thenReturn(futureMock);
		return remoteExecutionManagerMock;
	}

	private void configureMojo(RunAutomationPackageMojoTestable mojo, boolean ensureExecutionSuccess) throws URISyntaxException {
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
		mojo.setEnsureExecutionSuccess(ensureExecutionSuccess);

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

	private static class RunAutomationPackageMojoTestable extends ExecuteAutomationPackageMojo {

		private final RemoteExecutionManager remoteExecutionManagerMock;
		private final RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock;

		public RunAutomationPackageMojoTestable(RemoteExecutionManager remoteExecutionManagerMock, RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock) {
			super();
			this.remoteExecutionManagerMock = remoteExecutionManagerMock;
			this.remoteAutomationPackageClientMock = remoteAutomationPackageClientMock;
		}

		@Override
		protected RemoteExecutionManager createRemoteExecutionManager() {
			return remoteExecutionManagerMock;
		}

		@Override
		protected RemoteAutomationPackageClientImpl createRemoteAutomationPackageClient() {
			return remoteAutomationPackageClientMock;
		}

    }
}