package step.plugins.maven;

import org.apache.maven.artifact.Artifact;
import org.bson.types.ObjectId;
import org.mockito.Mockito;
import step.controller.multitenancy.Tenant;

import java.io.File;
import java.net.URISyntaxException;

public abstract class AbstractMojoTest {

	public static final String GROUP_ID = "my-group-id";
	public static final String ARTIFACT_ID = "step-functions-plugins-java-handler-test-3";
	public static final String VERSION_ID = "0.0.0-SNAPSHOT";

	protected static final Tenant TENANT_1 = createTenant1();
	protected static final Tenant TENANT_2 = createTenant2();

	protected Artifact createArtifactMock() throws URISyntaxException {
		Artifact mainArtifact = Mockito.mock(Artifact.class);
		Mockito.when(mainArtifact.getArtifactId()).thenReturn(ARTIFACT_ID);
		Mockito.when(mainArtifact.getClassifier()).thenReturn("jar");
		Mockito.when(mainArtifact.getGroupId()).thenReturn(GROUP_ID);
		Mockito.when(mainArtifact.getVersion()).thenReturn(VERSION_ID);
		Mockito.when(mainArtifact.getFile()).thenReturn(new File(this.getClass().getClassLoader().getResource("step/plugins/maven/test-file-jar.jar").toURI()));
		return mainArtifact;
	}

	protected Artifact createArtifactWithDependenciesMock() throws URISyntaxException {
		Artifact jarWithDependenciesArtifact = Mockito.mock(Artifact.class);
		Mockito.when(jarWithDependenciesArtifact.getArtifactId()).thenReturn(ARTIFACT_ID);
		Mockito.when(jarWithDependenciesArtifact.getClassifier()).thenReturn("jar-with-dependencies");
		Mockito.when(jarWithDependenciesArtifact.getGroupId()).thenReturn(GROUP_ID);
		Mockito.when(jarWithDependenciesArtifact.getVersion()).thenReturn(VERSION_ID);
		Mockito.when(jarWithDependenciesArtifact.getFile()).thenReturn(new File(this.getClass().getClassLoader().getResource("step/plugins/maven/test-file-jar-with-dependencies.jar").toURI()));
		return jarWithDependenciesArtifact;
	}

	protected static Tenant createTenant1() {
		Tenant tenant1 = new Tenant();
		tenant1.setName("project1");
		tenant1.setProjectId(new ObjectId().toString());
		return tenant1;
	}

	protected static Tenant createTenant2() {
		Tenant tenant2 = new Tenant();
		tenant2.setName("project2");
		tenant2.setProjectId(new ObjectId().toString());
		return tenant2;
	}
}
