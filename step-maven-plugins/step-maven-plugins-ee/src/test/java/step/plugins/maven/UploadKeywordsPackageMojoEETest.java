package step.plugins.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;

public class UploadKeywordsPackageMojoEETest {

	@Test
	@Ignore
	public void testExecuteOk() throws MojoExecutionException, MojoFailureException, URISyntaxException {
		UploadKeywordsPackageMojoEE mojo = new UploadKeywordsPackageMojoEE();

		MavenProject mockedProject = Mockito.mock(MavenProject.class);

		String artifactId = "step-functions-plugins-java-handler-test-3";
		Mockito.when(mockedProject.getArtifactId()).thenReturn(artifactId);

		Artifact mainArtifact = Mockito.mock(Artifact.class);
		Mockito.when(mainArtifact.getArtifactId()).thenReturn(artifactId);
		Mockito.when(mainArtifact.getClassifier()).thenReturn("jar");
		Mockito.when(mainArtifact.getGroupId()).thenReturn("test-group-id");
		Mockito.when(mainArtifact.getVersion()).thenReturn("0.0.0-SNAPSHOT");
		Mockito.when(mainArtifact.getFile()).thenReturn(new File(this.getClass().getClassLoader().getResource("step/plugins/maven/test-file-jar.jar").toURI()));

		Mockito.when(mockedProject.getArtifact()).thenReturn(mainArtifact);

		mojo.setUrl("http://localhost:4201");
		mojo.setAuthToken("eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjI4MGNhYS1jZDNjLTQxOTgtYTVhOS03ZDY4ZGYyYjhlNTMiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwOTAiLCJhdWQiOiJodHRwOi8vbG9jYWxob3N0OjgwOTAiLCJzdWIiOiJhZG1pbiIsIm5iZiI6MTY3NzE0OTU2NiwiaWF0IjoxNjc3MTQ5NTY2LCJleHAiOjE2Nzk3NDE1NjYsImlzTG9jYWxUb2tlbiI6dHJ1ZX0.3m75q3jHz4Qfbpwa5mCrwNHy4z-6w7aKSAQ6AAabLI0");
		HashMap<String, String> customAttributes = new HashMap<>();
		customAttributes.put("artifactId", artifactId);
		customAttributes.put("versionId", "0.0.0-SNAPSHOT");

		mojo.setCustomPackageAttributes(customAttributes);
		mojo.setStepProjectId("63dbe1042a5b7a70e3cbf99c");

		mojo.setProject(mockedProject);

		mojo.execute();

	}
}