package step.plugins.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Ignore;
import org.junit.Test;

public class StepFunctionsDeployMojoTest {

	// TODO: just for local testing
	@Test
	@Ignore
	public void testExecute() throws MojoExecutionException {
		RunExecutionBundleMojo mojo = new RunExecutionBundleMojo();
		mojo.setUrl("http://localhost:4201");
		mojo.setArtifactId("step-functions-plugins-java-handler-test");
		mojo.setArtifactVersion("0.0.0-20230218.123842-1");
		mojo.setDescription("iegorov-plugin-test");
		mojo.setUserId("me");
		mojo.setArtifactClassifier("jar-with-dependencies");

		mojo.execute();
	}
}