package step.cli.parameters;

public class ApDeployParameters extends ApParameters<ApDeployParameters> {
    private Boolean async;
    private String versionName;
    private String activationExpression;
    private Boolean forceRefreshOfSnapshots;
    private Integer deploymentTimeout;

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

    public Boolean getForceRefreshOfSnapshots() {
        return forceRefreshOfSnapshots;
    }

    public ApDeployParameters setForceRefreshOfSnapshots(Boolean forceRefreshOfSnapshots) {
        this.forceRefreshOfSnapshots = forceRefreshOfSnapshots;
        return this;
    }

}
