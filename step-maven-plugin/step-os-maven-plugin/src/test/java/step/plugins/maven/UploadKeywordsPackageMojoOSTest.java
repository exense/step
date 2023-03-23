package step.plugins.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.mockito.Mockito;
import step.core.accessors.AbstractAccessor;
import step.functions.packages.FunctionPackage;
import step.functions.packages.client.RemoteFunctionPackageClientImpl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class UploadKeywordsPackageMojoOSTest extends AbstractMojoTest {

	private static final FunctionPackage OLD_PACKAGE = createOldPackageMock();

	private static FunctionPackage createOldPackageMock() {
		FunctionPackage res = Mockito.mock(FunctionPackage.class);
		Mockito.when(res.getId()).thenReturn(new ObjectId(Date.from(LocalDateTime.of(2023, 3, 10, 15, 55, 55).toInstant(ZoneOffset.UTC))));
		return res;
	}

	private static final FunctionPackage UPDATED_PACKAGE = createUpdatedPackageMock();

	private static FunctionPackage createUpdatedPackageMock() {
		FunctionPackage res = Mockito.mock(FunctionPackage.class);
		Mockito.when(res.getId()).thenReturn(new ObjectId(Date.from(LocalDateTime.of(2023, 3, 10, 15, 55, 55).toInstant(ZoneOffset.UTC))));
		return res;
	}

	@Test
	public void testExecuteOk() throws MojoExecutionException, MojoFailureException, URISyntaxException, IOException {
		UploadKeywordsPackageMojoOSTestable mojo = new UploadKeywordsPackageMojoOSTestable(createRemoteFunctionManagerMock(), createRemoteFunctionAccessorMock());
		configureMojo(mojo);

		MavenProject mockedProject = Mockito.mock(MavenProject.class);

		String artifactId = "step-functions-plugins-java-handler-test-3";
		Mockito.when(mockedProject.getArtifactId()).thenReturn(artifactId);

		Artifact mainArtifact = createArtifactMock();
		Mockito.when(mockedProject.getArtifact()).thenReturn(mainArtifact);

		mojo.setUrl("http://localhost:4201");
		HashMap<String, String> customAttributes = new HashMap<>();
		customAttributes.put("artifactId", artifactId);
		customAttributes.put("versionId", "0.0.0-SNAPSHOT");

		mojo.setCustomPackageAttributes(customAttributes);

		mojo.setProject(mockedProject);

		mojo.execute();

		// TODO: verify arguments
	}

	private void configureMojo(UploadKeywordsPackageMojoOSTestable mojo) throws URISyntaxException {
		mojo.setUrl("http://localhost:8080");
		mojo.setBuildFinalName("Test build name");
		mojo.setProjectVersion("1.0.0");

		MavenProject mockedProject = Mockito.mock(MavenProject.class);

		Artifact mainArtifact = createArtifactMock();

		Mockito.when(mockedProject.getArtifact()).thenReturn(mainArtifact);

		Mockito.when(mockedProject.getArtifacts()).thenReturn(new HashSet<>());

		Artifact jarWithDependenciesArtifact = createArtifactWithDependenciesMock();

		Mockito.when(mockedProject.getAttachedArtifacts()).thenReturn(Arrays.asList(jarWithDependenciesArtifact));

		mojo.setProject(mockedProject);
	}

	private RemoteFunctionPackageClientImpl createRemoteFunctionManagerMock() throws IOException {
		RemoteFunctionPackageClientImpl remoteFunctionPackageClient = Mockito.mock(RemoteFunctionPackageClientImpl.class);
		FunctionPackage updatedPackage = UPDATED_PACKAGE;
		Mockito.when(remoteFunctionPackageClient.updateKeywordPackageById(Mockito.any(), Mockito.isNull(), Mockito.any(), Mockito.any())).thenReturn(updatedPackage);
		return remoteFunctionPackageClient;
	}

	private AbstractAccessor<FunctionPackage> createRemoteFunctionAccessorMock(){
		AbstractAccessor<FunctionPackage> mock = Mockito.mock(AbstractAccessor.class);
		FunctionPackage functionPackage = OLD_PACKAGE;
		Mockito.when(mock.findByCriteria(Mockito.any())).thenReturn(functionPackage);
		return mock;
	}

	private static class UploadKeywordsPackageMojoOSTestable extends UploadKeywordsPackageMojoOS {

		private final RemoteFunctionPackageClientImpl functionPackageClientMock;
		private final AbstractAccessor<FunctionPackage> remoteFunctionAccessor;

		public UploadKeywordsPackageMojoOSTestable(RemoteFunctionPackageClientImpl functionPackageClientMock, AbstractAccessor<FunctionPackage> remoteFunctionAccessor) {
			this.functionPackageClientMock = functionPackageClientMock;
			this.remoteFunctionAccessor = remoteFunctionAccessor;
		}

		@Override
		protected RemoteFunctionPackageClientImpl createRemoteFunctionPackageClient() {
			return functionPackageClientMock;
		}

		@Override
		protected AbstractAccessor<FunctionPackage> createRemoteFunctionPackageAccessor() {
			return remoteFunctionAccessor;
		}
	}
}