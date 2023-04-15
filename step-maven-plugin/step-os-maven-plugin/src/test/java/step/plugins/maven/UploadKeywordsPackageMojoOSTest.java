package step.plugins.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import step.core.accessors.AbstractAccessor;
import step.functions.packages.FunctionPackage;
import step.functions.packages.client.RemoteFunctionPackageClientImpl;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class UploadKeywordsPackageMojoOSTest extends AbstractMojoTest {

	private static final FunctionPackage OLD_PACKAGE = createOldPackageMock();
	private static final FunctionPackage UPDATED_PACKAGE = createUpdatedPackageMock();

	private static final String GROUP_ID = "my-group-id";
	private static final String ARTIFACT_ID = "step-functions-plugins-java-handler-test-3";
	private static final String VERSION_ID = "0.0.0-SNAPSHOT";

	private static FunctionPackage createOldPackageMock() {
		FunctionPackage res = Mockito.mock(FunctionPackage.class);
		Mockito.when(res.getId()).thenReturn(new ObjectId(Date.from(LocalDateTime.of(2023, 3, 10, 15, 55, 55).toInstant(ZoneOffset.UTC))));
		return res;
	}

	private static FunctionPackage createUpdatedPackageMock() {
		FunctionPackage res = Mockito.mock(FunctionPackage.class);
		Mockito.when(res.getId()).thenReturn(new ObjectId(Date.from(LocalDateTime.of(2023, 3, 10, 15, 55, 55).toInstant(ZoneOffset.UTC))));
		return res;
	}

	@Test
	public void testExecuteOk() throws MojoExecutionException, MojoFailureException, URISyntaxException, IOException {
		AbstractAccessor<FunctionPackage> functionAccessorMock = createRemoteFunctionAccessorMock();
		RemoteFunctionPackageClientImpl remoteFunctionManagerMock = createRemoteFunctionManagerMock();
		UploadKeywordsPackageMojoOSTestable mojo = new UploadKeywordsPackageMojoOSTestable(remoteFunctionManagerMock, functionAccessorMock);

		// configure mojo with test parameters and mocked Maven Project
		configureMojo(mojo);

		mojo.execute();

		// verify arguments of external calls
		ArgumentCaptor<Map<String, String>> searchCriteriaCaptor = ArgumentCaptor.forClass(Map.class);
		Mockito.verify(functionAccessorMock, Mockito.times(1)).findByCriteria(searchCriteriaCaptor.capture());
		Assert.assertEquals(Set.of("customFields.tracking"), searchCriteriaCaptor.getValue().keySet());
		Assert.assertEquals(GROUP_ID + "." + ARTIFACT_ID, searchCriteriaCaptor.getValue().get("customFields.tracking"));

		Mockito.verifyNoMoreInteractions(functionAccessorMock);

		ArgumentCaptor<FunctionPackage> oldPackageCaptor = ArgumentCaptor.forClass(FunctionPackage.class);
		ArgumentCaptor<File> uploadedFileCaptor = ArgumentCaptor.forClass(File.class);
		ArgumentCaptor<Map<String, String>> uploadedPackageAttributesCaptor = ArgumentCaptor.forClass(Map.class);
		Mockito.verify(remoteFunctionManagerMock, Mockito.times(1)).updateKeywordPackageById(
				oldPackageCaptor.capture(),
				Mockito.isNull(),
				uploadedFileCaptor.capture(),
				uploadedPackageAttributesCaptor.capture(),
				Mockito.eq(GROUP_ID + "." + ARTIFACT_ID)
		);
		Assert.assertEquals(OLD_PACKAGE, oldPackageCaptor.getValue());

		// by default, we take 'jar' artifact (if the classifier is not specified)
		Assert.assertEquals("test-file-jar.jar", uploadedFileCaptor.getValue().getName());
		Assert.assertEquals(Set.of("versionId", "artifactId"), uploadedPackageAttributesCaptor.getValue().keySet());
		Assert.assertEquals(VERSION_ID, uploadedPackageAttributesCaptor.getValue().get("versionId"));
		Assert.assertEquals(ARTIFACT_ID, uploadedPackageAttributesCaptor.getValue().get("artifactId"));

		Mockito.verify(remoteFunctionManagerMock).close();

		Mockito.verifyNoMoreInteractions(remoteFunctionManagerMock);
	}

	private void configureMojo(UploadKeywordsPackageMojoOSTestable mojo) throws URISyntaxException {
		mojo.setUrl("http://localhost:8080");
		mojo.setBuildFinalName("Test build name");
		mojo.setProjectVersion("1.0.0");
		mojo.setArtifactId(ARTIFACT_ID);
		mojo.setArtifactVersion(VERSION_ID);
		mojo.setGroupId(GROUP_ID);

		MavenProject mockedProject = Mockito.mock(MavenProject.class);

		Artifact mainArtifact = createArtifactMock();

		Mockito.when(mockedProject.getArtifact()).thenReturn(mainArtifact);
		Mockito.when(mockedProject.getArtifacts()).thenReturn(new HashSet<>());
		Mockito.when(mockedProject.getArtifactId()).thenReturn(ARTIFACT_ID);
		Mockito.when(mockedProject.getGroupId()).thenReturn(GROUP_ID);

		Artifact jarWithDependenciesArtifact = createArtifactWithDependenciesMock();

		Mockito.when(mockedProject.getAttachedArtifacts()).thenReturn(Arrays.asList(jarWithDependenciesArtifact));

		mojo.setProject(mockedProject);

		HashMap<String, String> customAttributes = new HashMap<>();
		customAttributes.put("artifactId", ARTIFACT_ID);
		customAttributes.put("versionId", VERSION_ID);

		mojo.setCustomPackageAttributes(customAttributes);
	}

	private RemoteFunctionPackageClientImpl createRemoteFunctionManagerMock() throws IOException {
		RemoteFunctionPackageClientImpl remoteFunctionPackageClient = Mockito.mock(RemoteFunctionPackageClientImpl.class);
		Mockito.when(remoteFunctionPackageClient.updateKeywordPackageById(
				Mockito.any(),
				Mockito.isNull(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any())
		).thenReturn(UPDATED_PACKAGE);
		return remoteFunctionPackageClient;
	}

	private AbstractAccessor<FunctionPackage> createRemoteFunctionAccessorMock(){
		AbstractAccessor<FunctionPackage> mock = Mockito.mock(AbstractAccessor.class);
		Mockito.when(mock.findByCriteria(Mockito.any())).thenReturn(OLD_PACKAGE);
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