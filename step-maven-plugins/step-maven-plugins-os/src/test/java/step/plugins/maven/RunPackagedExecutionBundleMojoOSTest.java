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
import step.resources.Resource;
import step.resources.SimilarResourceExistingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class RunPackagedExecutionBundleMojoOSTest {

	@Test
	public void testExecuteOk() throws InterruptedException, TimeoutException, MojoExecutionException, MojoFailureException, URISyntaxException, SimilarResourceExistingException, IOException {
		RemoteExecutionManager remoteExecutionManagerMock = createExecutionManagerMock(ReportNodeStatus.PASSED);

		RemoteResourceManager remoteResourceManagerMock = Mockito.mock(RemoteResourceManager.class);
		Resource resourceMock = Mockito.mock(Resource.class);
		Mockito.when(resourceMock.getId()).thenReturn(new ObjectId());

		Mockito.when(remoteResourceManagerMock.createResource(Mockito.anyString(), Mockito.any(InputStream.class), Mockito.anyString(), Mockito.anyBoolean(), Mockito.isNull())).thenReturn(resourceMock);

		RunPackagedExecutionBundleMojoTestable mojo = new RunPackagedExecutionBundleMojoTestable(remoteExecutionManagerMock, remoteResourceManagerMock);
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
		Assert.assertEquals("testUser", captured.getUserID());
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

		Mockito.when(remoteExecutionManagerMock.getFuture("exec-id-1")).thenReturn(remoteExecutionFuture);
		return remoteExecutionManagerMock;
	}

	private void configureMojo(RunPackagedExecutionBundleMojoTestable mojo) throws URISyntaxException {
		mojo.setArtifactId("test-artifact-id");
		mojo.setArtifactClassifier("jar-with-dependencies");
		mojo.setArtifactVersion("1.0.0-RELEASE");
		mojo.setGroupId("test-group-id");
		mojo.setDescription("Test description");
		mojo.setUrl("http://localhost:8080");
//		mojo.setAuthToken("abc");
		mojo.setBuildFinalName("Test build name");
		mojo.setProjectVersion("1.0.0");
		mojo.setCheckExecutionResult(true);
		mojo.setExecutionResultTimeoutS(3);
		mojo.setUserId("testUser");
		Map<String, String> params = createTestCustomParams();
		mojo.setCustomParameters(params);

		MavenProject mockedProject = Mockito.mock(MavenProject.class);

		Artifact mainArtifact = Mockito.mock(Artifact.class);
		Mockito.when(mainArtifact.getArtifactId()).thenReturn("test-artifact-id");
		Mockito.when(mainArtifact.getClassifier()).thenReturn("jar");
		Mockito.when(mainArtifact.getGroupId()).thenReturn("test-group-id");
		Mockito.when(mainArtifact.getVersion()).thenReturn("1.0.0-RELEASE");
		Mockito.when(mainArtifact.getFile()).thenReturn(new File(this.getClass().getClassLoader().getResource("step/plugins/maven/test-file-jar.jar").toURI()));
		Mockito.when(mockedProject.getArtifact()).thenReturn(mainArtifact);

		Mockito.when(mockedProject.getArtifacts()).thenReturn(new HashSet<>());

		Artifact jarWithDependenciesArtifact = Mockito.mock(Artifact.class);
		Mockito.when(jarWithDependenciesArtifact.getArtifactId()).thenReturn("test-artifact-id");
		Mockito.when(jarWithDependenciesArtifact.getClassifier()).thenReturn("jar-with-dependencies");
		Mockito.when(jarWithDependenciesArtifact.getGroupId()).thenReturn("test-group-id");
		Mockito.when(jarWithDependenciesArtifact.getVersion()).thenReturn("1.0.0-RELEASE");
		Mockito.when(jarWithDependenciesArtifact.getFile()).thenReturn(new File(this.getClass().getClassLoader().getResource("step/plugins/maven/test-file-jar-with-dependencies.jar").toURI()));

		Mockito.when(mockedProject.getAttachedArtifacts()).thenReturn(Arrays.asList(jarWithDependenciesArtifact));

		mojo.setProject(mockedProject);
	}

	private static Map<String, String> createTestCustomParams() {
		Map<String, String> params = new HashMap<>();
		params.put("param1", "value1");
		params.put("param2", "value2");
		return params;
	}

	private static class RunPackagedExecutionBundleMojoTestable extends RunPackagedExecutionBundleMojoOS {

		private RemoteExecutionManager remoteExecutionManagerMock;
		private RemoteResourceManager remoteResourceManagerMock;

		public RunPackagedExecutionBundleMojoTestable(RemoteExecutionManager remoteExecutionManagerMock, RemoteResourceManager remoteResourceManagerMock) {
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