package step.plugins.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import step.core.repositories.RepositoryObjectReference;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractRunDeployedAutomationPackagesMojo extends AbstractRunAutomationPackagesMojo {

	@Parameter(property = "step-run-auto-packages.step-maven-settings", required = false)
	private String stepMavenSettings;

	public AbstractRunDeployedAutomationPackagesMojo() {
	}

	public void execute() throws MojoExecutionException {
		getLog().info("Run Step execution for deployed module " + getBuildFinalName() + " (version=" + getProjectVersion() + ")");

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

	public String getStepMavenSettings() {
		return stepMavenSettings;
	}

	public void setStepMavenSettings(String stepMavenSettings) {
		this.stepMavenSettings = stepMavenSettings;
	}

}