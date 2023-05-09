/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.plugins.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import step.constants.ArtifactConstants;
import step.core.repositories.RepositoryObjectReference;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractRunDeployedAutomationPackagesMojo extends AbstractRunAutomationPackagesMojo {

	@Parameter(property = "step-run-auto-packages.step-maven-settings", required = false)
	private String stepMavenSettings;

	public AbstractRunDeployedAutomationPackagesMojo() {
	}

	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Run Step execution for deployed module " + getBuildFinalName() + " (version=" + getProjectVersion() + ") on " + getUrl() + " ");

		// empty context here - we just call the execution client according to the  plugin parameters
		executeBundleOnStep(new HashMap<>());
	}

	@Override
	protected RepositoryObjectReference prepareExecutionRepositoryObject(Map<String, Object> executionContext) {
		return new RepositoryObjectReference("Artifact", prepareRepositoryParameters());
	}

	private HashMap<String, String> prepareRepositoryParameters() {
		HashMap<String, String> repoParams = new HashMap<>();
		repoParams.put(ArtifactConstants.PARAM_GROUP_ID, getGroupId());
		repoParams.put(ArtifactConstants.PARAM_ARTIFACT_ID, getArtifactId());
		repoParams.put(ArtifactConstants.PARAM_VERSION, getArtifactVersion());
		if (getArtifactClassifier() != null && !getArtifactClassifier().isEmpty()) {
			repoParams.put(ArtifactConstants.PARAM_CLASSIFIER, getArtifactClassifier());
		}
		if (getStepMavenSettings() != null && !getStepMavenSettings().isEmpty()) {
			repoParams.put(ArtifactConstants.PARAM_MAVEN_SETTINGS, getStepMavenSettings());
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