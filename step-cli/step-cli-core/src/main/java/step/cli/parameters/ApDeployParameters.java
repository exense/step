package step.cli.parameters;

import java.util.Map;

public class ApDeployParameters extends ApParameters<ApDeployParameters> {
    private Boolean async;
    private String versionName;
    private String activationExpression;
    private Boolean forceRefreshOfSnapshots;
    private Integer deploymentTimeout;
    private Map<String, String> plansAttributes;
    private Map<String, String> functionsAttributes;
    private Map<String, String> tokenSelectionCriteria;
    private Boolean executeFunctionsLocally;

    public Boolean getAsync() {
        return async;
    }

    /**
     * @return the max time (in seconds) to wait for the deployment to complete on the server, or {@code null} to use the client default.
     */
    public Integer getDeploymentTimeout() {
        return deploymentTimeout;
    }

    public ApDeployParameters setDeploymentTimeout(Integer deploymentTimeout) {
        this.deploymentTimeout = deploymentTimeout;
        return this;
    }

    public ApDeployParameters setAsync(Boolean async) {
        this.async = async;
        return this;
    }

    public String getVersionName() {
        return versionName;
    }

    public ApDeployParameters setVersionName(String versionName) {
        this.versionName = versionName;
        return this;
    }

    public String getActivationExpression() {
        return activationExpression;
    }

    public ApDeployParameters setActivationExpression(String activationExpression) {
        this.activationExpression = activationExpression;
        return this;
    }

    /**
     * @return the attributes to be applied to all the plans of the package, or {@code null} to apply none.
     */
    public Map<String, String> getPlansAttributes() {
        return plansAttributes;
    }

    public ApDeployParameters setPlansAttributes(Map<String, String> plansAttributes) {
        this.plansAttributes = plansAttributes;
        return this;
    }

    /**
     * @return the attributes to be applied to all the keywords of the package, or {@code null} to apply none.
     */
    public Map<String, String> getFunctionsAttributes() {
        return functionsAttributes;
    }

    public ApDeployParameters setFunctionsAttributes(Map<String, String> functionsAttributes) {
        this.functionsAttributes = functionsAttributes;
        return this;
    }

    /**
     * @return the token selection criteria to be applied to all the keywords of the package, or {@code null} to apply
     * none.
     */
    public Map<String, String> getTokenSelectionCriteria() {
        return tokenSelectionCriteria;
    }

    public ApDeployParameters setTokenSelectionCriteria(Map<String, String> tokenSelectionCriteria) {
        this.tokenSelectionCriteria = tokenSelectionCriteria;
        return this;
    }

    /**
     * @return whether all the keywords of the package have to be executed locally (i.e. on the controller).
     */
    public Boolean getExecuteFunctionsLocally() {
        return executeFunctionsLocally;
    }

    public ApDeployParameters setExecuteFunctionsLocally(Boolean executeFunctionsLocally) {
        this.executeFunctionsLocally = executeFunctionsLocally;
        return this;
    }

    public Boolean getForceRefreshOfSnapshots() {
        return forceRefreshOfSnapshots;
    }

    public ApDeployParameters setForceRefreshOfSnapshots(Boolean forceRefreshOfSnapshots) {
        this.forceRefreshOfSnapshots = forceRefreshOfSnapshots;
        return this;
    }

}
