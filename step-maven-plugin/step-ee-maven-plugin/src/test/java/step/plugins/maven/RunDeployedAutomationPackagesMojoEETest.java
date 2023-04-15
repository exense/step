package step.plugins.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import step.client.executions.RemoteExecutionFuture;
import step.client.executions.RemoteExecutionManager;
import step.controller.multitenancy.client.MultitenancyClient;
import step.controller.multitenancy.client.RemoteMultitenancyClientImpl;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class RunDeployedAutomationPackagesMojoEETest extends AbstractMojoTest {

	/**
	 * Checks if the mojo calls the underlying {@link RemoteExecutionManager} with valid parameters
	 */
	@Test
	public void testExecuteOk() throws MojoExecutionException, InterruptedException, TimeoutException {
		RemoteExecutionManager remoteExecutionManagerMock = createExecutionManagerMock(ReportNodeStatus.PASSED);
		RemoteMultitenancyClientImpl multitenancyClientMock = createRemoteMultitenancyClientMock();

		RunDeployedAutomationPackagesMojoTestable mojo = new RunDeployedAutomationPackagesMojoTestable(remoteExecutionManagerMock, multitenancyClientMock);
		configureMojo(mojo);

		mojo.execute();

		ArgumentCaptor<ExecutionParameters> captor = ArgumentCaptor.forClass(ExecutionParameters.class);
		Mockito.verify(remoteExecutionManagerMock, Mockito.times(1)).execute(captor.capture());

		ExecutionParameters captured = captor.getValue();
		Assert.assertEquals("Test description", captured.getDescription());
		Assert.assertEquals("testUser", captured.getUserID());
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
		RemoteMultitenancyClientImpl multitenancyClientMock = createRemoteMultitenancyClientMock();

		RunDeployedAutomationPackagesMojoTestable mojo = new RunDeployedAutomationPackagesMojoTestable(remoteExecutionManagerMock, multitenancyClientMock);
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

	private void configureMojo(RunDeployedAutomationPackagesMojoTestable mojo) {
		mojo.setArtifactId(ARTIFACT_ID);
		mojo.setArtifactClassifier("jar-with-dependencies");
		mojo.setArtifactVersion(VERSION_ID);
		mojo.setGroupId(GROUP_ID);
		mojo.setDescription("Test description");
		mojo.setUrl("http://localhost:8080");
		mojo.setAuthToken("abc");
		mojo.setBuildFinalName("Test build name");
		mojo.setProjectVersion("1.0.0");
		mojo.setExecutionResultTimeoutS(3);
		mojo.setStepMavenSettings("default");
		mojo.setUserId("testUser");
		mojo.setWaitForExecution(true);
		mojo.setEnsureExecutionSuccess(true);

		Map<String, String> params = createTestCustomParams();
		mojo.setExecutionParameters(params);

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

	private static class RunDeployedAutomationPackagesMojoTestable extends RunDeployedAutomationPackagesMojoEE {

		private final RemoteExecutionManager remoteExecutionManagerMock;
		private final MultitenancyClient multitenancyClientMock;

		public RunDeployedAutomationPackagesMojoTestable(RemoteExecutionManager remoteExecutionManagerMock, MultitenancyClient multitenancyClientMock) {
			super();
			this.remoteExecutionManagerMock = remoteExecutionManagerMock;
			this.multitenancyClientMock = multitenancyClientMock;
		}

		@Override
		protected RemoteExecutionManager createRemoteExecutionManager() {
			return remoteExecutionManagerMock;
		}

		@Override
		protected MultitenancyClient createMultitenancyClient() {
			return multitenancyClientMock;
		}
	}

}