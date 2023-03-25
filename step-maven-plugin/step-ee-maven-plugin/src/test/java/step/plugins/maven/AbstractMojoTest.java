package step.plugins.maven;

import org.apache.maven.artifact.Artifact;
import org.mockito.Mockito;

import java.io.File;
import java.net.URISyntaxException;

public abstract class AbstractMojoTest {
	protected Artifact createArtifactMock() throws URISyntaxException {
		Artifact mainArtifact = Mockito.mock(Artifact.class);
		Mockito.when(mainArtifact.getArtifactId()).thenReturn("test-artifact-id");
		Mockito.when(mainArtifact.getClassifier()).thenReturn("jar");
		Mockito.when(mainArtifact.getGroupId()).thenReturn("test-group-id");
		Mockito.when(mainArtifact.getVersion()).thenReturn("1.0.0-RELEASE");
		Mockito.when(mainArtifact.getFile()).thenReturn(new File(this.getClass().getClassLoader().getResource("step/plugins/maven/test-file-jar.jar").toURI()));
		return mainArtifact;
	}

	protected Artifact createArtifactWithDependenciesMock() throws URISyntaxException {
		Artifact jarWithDependenciesArtifact = Mockito.mock(Artifact.class);
		Mockito.when(jarWithDependenciesArtifact.getArtifactId()).thenReturn("test-artifact-id");
		Mockito.when(jarWithDependenciesArtifact.getClassifier()).thenReturn("jar-with-dependencies");
		Mockito.when(jarWithDependenciesArtifact.getGroupId()).thenReturn("test-group-id");
		Mockito.when(jarWithDependenciesArtifact.getVersion()).thenReturn("1.0.0-RELEASE");
		Mockito.when(jarWithDependenciesArtifact.getFile()).thenReturn(new File(this.getClass().getClassLoader().getResource("step/plugins/maven/test-file-jar-with-dependencies.jar").toURI()));
		return jarWithDependenciesArtifact;
	}
}
