package step.plugins.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import step.core.repositories.RepositoryObjectReference;

import java.util.HashMap;
import java.util.Map;

@Mojo(name = "run-deployed-execution-bundle")
public class RunDeployedExecutionBundleMojo extends AbstractRunExecutionBundleMojo {

	public RunDeployedExecutionBundleMojo() {
	}

	public void execute() throws MojoExecutionException {
		getLog().info("Run step execution for deployed module " + getBuildFinalName() + " (version=" + getProjectVersion() + ")");

		// empty context here - we just call the execution client according to the  plugin parameters
		executeBundleOnStep(new HashMap<>());
	}

	@Override
	protected RepositoryObjectReference prepareExecutionRepositoryObject(Map<String, Object> executionContext) {
		return new RepositoryObjectReference("Artifact", prepareRepositoryParameters());
	}

	private HashMap<String, String> prepareRepositoryParameters() {
		HashMap<String, String> repoParams = new HashMap<>();
		repoParams.put("groupId", getGroupId());
		repoParams.put("artifactId", getArtifactId());
		repoParams.put("version", getArtifactVersion());
		if (getArtifactClassifier() != null && !getArtifactClassifier().isEmpty()) {
			repoParams.put("classifier", getArtifactClassifier());
		}
		if (getStepMavenSettings() != null && !getStepMavenSettings().isEmpty()) {
			repoParams.put("mavenSettings", getStepMavenSettings());
		}
		return repoParams;
	}

}