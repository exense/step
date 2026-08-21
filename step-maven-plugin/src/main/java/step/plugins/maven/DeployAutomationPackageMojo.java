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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import step.cli.DeployAutomationPackageTool;
import step.cli.StepCliExecutionException;
import step.cli.parameters.ApDeployParameters;
import step.client.credentials.ControllerCredentials;
import step.core.maven.MavenArtifactIdentifier;

import java.io.File;
import java.util.Map;

@Mojo(name = "deploy-automation-package")
public class DeployAutomationPackageMojo extends AbstractAutomationPackageMojo {

    private static final String PLANS_ATTRIBUTE_NAME = "plans attribute";
    private static final String KEYWORDS_ATTRIBUTE_NAME = "keywords attribute";
    private static final String TOKEN_SELECTION_CRITERION_NAME = "token selection criterion";

    @Parameter(property = "step-deploy-automation-package.async")
    private Boolean async;
    @Parameter(property = "step-deploy-automation-package.version-name")
    private String versionName;
    @Parameter(property = "step-deploy-automation-package.activation-expression")
    private String activationExpression;
    @Parameter(property = "step-deploy-auto-packages.artifact-group-id")
    private String artifactGroupId;
    @Parameter(property = "step-deploy-auto-packages.artifact-id")
    private String artifactId;
    @Parameter(property = "step-deploy-auto-packages.artifact-version")
    private String artifactVersion;
    @Parameter(property = "step-deploy-auto-packages.artifact-classifier", required = false)
    private String artifactClassifier;
    @Parameter(property = "step-deploy-auto-packages.artifact-type", required = false)
    private String artifactType;

    @Parameter
    private LibraryConfiguration library;
    //Individual property required to override values via system properties
    @Parameter(property = "step-deploy-auto-packages.library.groupId")
    private String libraryGroupId;
    @Parameter(property = "step-deploy-auto-packages.library.artifactId")
    private String libraryArtifactId;
    @Parameter(property = "step-deploy-auto-packages.library.version")
    private String libraryVersion;
    @Parameter(property = "step-deploy-auto-packages.library.classifier")
    private String libraryClassifier;
    @Parameter(property = "step-deploy-auto-packages.library.type")
    private String libraryType;
    @Parameter(property = "step-deploy-auto-packages.library.path")
    private String libraryPath;
    @Parameter(property = "step-deploy-auto-packages.library.managed")
    private String libraryManaged;

    @Parameter(property = "step-deploy-auto-packages.force-refresh-snapshots", required = false)
    private Boolean forceRefreshOfSnapshots;

    @Parameter(property = "step-deploy-automation-package.deployment-timeout", required = false)
    private Integer deploymentTimeout;

    @Parameter
    private Map<String, String> plansAttributes;
    // Individual string properties to support passing the maps above as system properties.
    // Format: key1=value1;key2=value2. When set, these values are merged with (and override) the ones from the pom.xml.
    @Parameter(property = "step-deploy-automation-package.plans-attributes")
    private String plansAttributesRaw;

    @Parameter
    private Map<String, String> keywordsAttributes;
    @Parameter(property = "step-deploy-automation-package.keywords-attributes")
    private String keywordsAttributesRaw;

    @Parameter
    private Map<String, String> tokenSelectionCriteria;
    @Parameter(property = "step-deploy-automation-package.token-selection-criteria")
    private String tokenSelectionCriteriaRaw;

    @Parameter(property = "step-deploy-automation-package.execute-keywords-on-controller")
    private Boolean executeKeywordsOnController;

    @Override
    protected ControllerCredentials getControllerCredentials() {
        String authToken = getAuthToken();
        return new ControllerCredentials(getUrl(), authToken == null || authToken.isEmpty() ? null : authToken);
    }

    @Override
    public void execute() throws MojoExecutionException {
        try {
            validateEEConfiguration(getStepProjectName(), getAuthToken());
            checkStepControllerVersion();
            library = prepareLibrary();
            createTool(getUrl(), getStepProjectName(), getAuthToken(), getAsync(), getVersionName(), getActivationExpression(), getForceRefreshOfSnapshots()).execute();
        } catch (StepCliExecutionException e) {
            throw new MojoExecutionException("Execution exception", e);
        } catch (Exception e) {
            throw logAndThrow("Unexpected error while uploading automation package to Step", e);
        }
    }

    protected DeployAutomationPackageTool createTool(final String url, final String projectName, final String authToken, final Boolean async,
                                                     final String apVersion, final String activationExpr, Boolean forceRefreshOfSnapshots) throws MojoExecutionException {
        return new MavenDeployAutomationPackageTool(
            url, buildDeployParameters(projectName, authToken, async, apVersion, activationExpr, forceRefreshOfSnapshots)
        );
    }

    protected ApDeployParameters buildDeployParameters(final String projectName, final String authToken, final Boolean async,
                                                       final String apVersion, final String activationExpr, Boolean forceRefreshOfSnapshots) throws MojoExecutionException {
        MavenArtifactIdentifier remoteApMavenIdentifier = getRemoteMavenIdentifier();
        File localApFile = remoteApMavenIdentifier != null ? null : DeployAutomationPackageMojo.this.getFileToUpload();

        LibraryConfiguration library = getLibrary();
        File libraryFile = library != null ? library.toFile() : null;
        MavenArtifactIdentifier libraryMavenArtifact = library != null ? library.toMavenArtifactIdentifier() : null;
        String libraryName = library != null && library.isManagedLibraryNameConfigured() ? library.getManaged() : null;

        return new ApDeployParameters()
            .setAutomationPackageMavenArtifact(remoteApMavenIdentifier)
            .setAutomationPackageFile(localApFile)
            .setLibraryFile(libraryFile)
            .setlibraryMavenArtifact(libraryMavenArtifact)
            .setManagedLibraryName(libraryName)
            .setStepProjectName(projectName)
            .setAuthToken(authToken)
            .setAsync(async)
            .setForceRefreshOfSnapshots(forceRefreshOfSnapshots)
            .setDeploymentTimeout(getDeploymentTimeout())
            .setVersionName(apVersion)
            .setActivationExpression(activationExpr)
            .setPlansAttributes(getPlansAttributes())
            .setKeywordsAttributes(getKeywordsAttributes())
            .setTokenSelectionCriteria(getTokenSelectionCriteria())
            .setExecuteKeywordsOnController(getExecuteKeywordsOnController());
    }

    protected File getFileToUpload() throws MojoExecutionException {
        Artifact artifact = getProjectArtifact(getArtifactClassifier());

        if (artifact == null || artifact.getFile() == null) {
            throw new MojoExecutionException("Unable to resolve artifact to upload.");
        }

        return artifact.getFile();
    }

    public String getArtifactClassifier() {
        return artifactClassifier;
    }

    public void setArtifactClassifier(String artifactClassifier) {
        this.artifactClassifier = artifactClassifier;
    }

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getActivationExpression() {
        return activationExpression;
    }

    public void setActivationExpression(String activationExpression) {
        this.activationExpression = activationExpression;
    }

    public String getArtifactGroupId() {
        return artifactGroupId;
    }

    public void setArtifactGroupId(String artifactGroupId) {
        this.artifactGroupId = artifactGroupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }

    @Override
    public LibraryConfiguration getLibrary() {
        return library;
    }

    @Override
    public String getLibraryGroupId() {
        return libraryGroupId;
    }

    @Override
    public String getLibraryArtifactId() {
        return libraryArtifactId;
    }

    @Override
    public String getLibraryVersion() {
        return libraryVersion;
    }

    @Override
    public String getLibraryClassifier() {
        return libraryClassifier;
    }

    @Override
    public String getLibraryType() {
        return libraryType;
    }

    @Override
    public String getLibraryPath() {
        return libraryPath;
    }

    @Override
    public String getLibraryManaged() {
        return libraryManaged;
    }

    public Boolean getForceRefreshOfSnapshots() {
        return forceRefreshOfSnapshots;
    }

    public void setForceRefreshOfSnapshots(Boolean forceRefreshOfSnapshots) {
        this.forceRefreshOfSnapshots = forceRefreshOfSnapshots;
    }

    public Integer getDeploymentTimeout() {
        return deploymentTimeout;
    }

    public void setDeploymentTimeout(Integer deploymentTimeout) {
        this.deploymentTimeout = deploymentTimeout;
    }

    public Map<String, String> getPlansAttributes() {
        return mergeWithRawValues(plansAttributes, plansAttributesRaw, PLANS_ATTRIBUTE_NAME);
    }

    public void setPlansAttributes(Map<String, String> plansAttributes) {
        this.plansAttributes = plansAttributes;
    }

    public void setPlansAttributesRaw(String plansAttributesRaw) {
        this.plansAttributesRaw = plansAttributesRaw;
    }

    public Map<String, String> getKeywordsAttributes() {
        return mergeWithRawValues(keywordsAttributes, keywordsAttributesRaw, KEYWORDS_ATTRIBUTE_NAME);
    }

    public void setKeywordsAttributes(Map<String, String> keywordsAttributes) {
        this.keywordsAttributes = keywordsAttributes;
    }

    public void setKeywordsAttributesRaw(String keywordsAttributesRaw) {
        this.keywordsAttributesRaw = keywordsAttributesRaw;
    }

    public Map<String, String> getTokenSelectionCriteria() {
        return mergeWithRawValues(tokenSelectionCriteria, tokenSelectionCriteriaRaw, TOKEN_SELECTION_CRITERION_NAME);
    }

    public void setTokenSelectionCriteria(Map<String, String> tokenSelectionCriteria) {
        this.tokenSelectionCriteria = tokenSelectionCriteria;
    }

    public void setTokenSelectionCriteriaRaw(String tokenSelectionCriteriaRaw) {
        this.tokenSelectionCriteriaRaw = tokenSelectionCriteriaRaw;
    }

    public Boolean getExecuteKeywordsOnController() {
        return executeKeywordsOnController;
    }

    public void setExecuteKeywordsOnController(Boolean executeKeywordsOnController) {
        this.executeKeywordsOnController = executeKeywordsOnController;
    }

    protected boolean isLocalMavenArtifact() {
        return getArtifactId() == null || getArtifactId().isEmpty() || getArtifactGroupId() == null || getArtifactGroupId().isEmpty();
    }

    /**
     * @return the identifier of the remote maven artifact (alternatively to the deployment of current maven artefact)
     */
    protected MavenArtifactIdentifier getRemoteMavenIdentifier() throws MojoExecutionException {
        MavenArtifactIdentifier remoteMavenArtifact = null;
        if (!isLocalMavenArtifact()) {
            remoteMavenArtifact = new MavenArtifactIdentifier(getArtifactGroupId(), getArtifactId(), getArtifactVersion(), getArtifactClassifier(), getArtifactType());
        }
        return remoteMavenArtifact;
    }

    protected class MavenDeployAutomationPackageTool extends DeployAutomationPackageTool {

        public MavenDeployAutomationPackageTool(String url, ApDeployParameters params) {
            super(url, params);
        }

        @Override
        public void logError(String errorText, Throwable e) {
            if (e != null) {
                DeployAutomationPackageMojo.this.getLog().error(errorText, e);
            } else {
                DeployAutomationPackageMojo.this.getLog().error(errorText);
            }
        }

        @Override
        public void logInfo(String infoText, Throwable e) {
            if (e != null) {
                DeployAutomationPackageMojo.this.getLog().info(infoText, e);
            } else {
                DeployAutomationPackageMojo.this.getLog().info(infoText);
            }
        }

        @Override
        public void logDebug(String infoText, Throwable e) {
            if (e != null) {
                DeployAutomationPackageMojo.this.getLog().debug(infoText, e);
            } else {
                DeployAutomationPackageMojo.this.getLog().debug(infoText);
            }
        }
    }
}
