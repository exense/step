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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import step.client.executions.RemoteExecutionFuture;
import step.client.executions.RemoteExecutionManager;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class RunDeployedAutomationPackagesMojoOSTest extends AbstractMojoTest {

	/**
	 * Checks if the mojo calls the underlying {@link RemoteExecutionManager} with valid parameters
	 */
	@Test
	public void testExecuteOk() throws MojoExecutionException, InterruptedException, TimeoutException {
		RemoteExecutionManager remoteExecutionManagerMock = createExecutionManagerMock(ReportNodeStatus.PASSED);

		RunDeployedAutomationPackagesMojoOSTestable mojo = new RunDeployedAutomationPackagesMojoOSTestable(remoteExecutionManagerMock);
		configureMojo(mojo);

		mojo.execute();

		ArgumentCaptor<ExecutionParameters> captor = ArgumentCaptor.forClass(ExecutionParameters.class);
		Mockito.verify(remoteExecutionManagerMock, Mockito.times(1)).execute(captor.capture());
		ExecutionParameters captured = captor.getValue();
		Assert.assertEquals("Test description", captured.getDescription());
		Assert.assertNull(captured.getUserID());
		Assert.assertEquals("Artifact", captured.getRepositoryObject().getRepositoryID());
		Assert.assertEquals(ExecutionMode.RUN, captured.getMode());
		Assert.assertEquals(ARTIFACT_ID, captured.getRepositoryObject().getRepositoryParameters().get("artifactId"));
		Assert.assertEquals(VERSION_ID, captured.getRepositoryObject().getRepositoryParameters().get("version"));
		Assert.assertEquals(GROUP_ID, captured.getRepositoryObject().getRepositoryParameters().get("groupId"));
		Assert.assertEquals("jar-with-dependencies", captured.getRepositoryObject().getRepositoryParameters().get("classifier"));
		Assert.assertEquals("default", captured.getRepositoryObject().getRepositoryParameters().get("mavenSettings"));
		Assert.assertEquals(createTestCustomParams(), captured.getCustomParameters());
	}

	/**
	 * Checks if the mojo stops the lifecycle if the underlying {@link RemoteExecutionManager} returns failed status
	 */
	@Test
	public void testExecuteNok() throws InterruptedException, TimeoutException {
		RemoteExecutionManager remoteExecutionManagerMock = createExecutionManagerMock(ReportNodeStatus.FAILED);

		RunDeployedAutomationPackagesMojoOSTestable mojo = new RunDeployedAutomationPackagesMojoOSTestable(remoteExecutionManagerMock);
		configureMojo(mojo);

		try {
			mojo.execute();
			Assert.fail("The maven lifecycle should be interrupted");
		} catch (MojoExecutionException ex){
			// ok
		}
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

		Mockito.when(remoteExecutionManagerMock.getFuture("exec-id-1")).thenReturn(remoteExecutionFuture);
		return remoteExecutionManagerMock;
	}

	private void configureMojo(RunDeployedAutomationPackagesMojoOSTestable mojo) {
		mojo.setArtifactId(ARTIFACT_ID);
		mojo.setArtifactClassifier("jar-with-dependencies");
		mojo.setArtifactVersion(VERSION_ID);
		mojo.setGroupId(GROUP_ID);
		mojo.setDescription("Test description");
		mojo.setUrl("http://localhost:8080");
		mojo.setBuildFinalName("Test build name");
		mojo.setProjectVersion("1.0.0");
		mojo.setExecutionResultTimeoutS(3);
		mojo.setStepMavenSettings("default");
		mojo.setWaitForExecution(true);
		mojo.setEnsureExecutionSuccess(true);

		Map<String, String> params = createTestCustomParams();
		mojo.setExecutionParameters(params);
	}

	private static Map<String, String> createTestCustomParams() {
		Map<String, String> params = new HashMap<>();
		params.put("param1", "value1");
		params.put("param2", "value2");
		return params;
	}

	private static class RunDeployedAutomationPackagesMojoOSTestable extends RunDeployedAutomationPackagesMojoOS {

		private RemoteExecutionManager remoteExecutionManagerMock;

		public RunDeployedAutomationPackagesMojoOSTestable(RemoteExecutionManager remoteExecutionManagerMock) {
			super();
			this.remoteExecutionManagerMock = remoteExecutionManagerMock;
		}

		@Override
		protected RemoteExecutionManager createRemoteExecutionManager() {
			return remoteExecutionManagerMock;
		}
	}
}