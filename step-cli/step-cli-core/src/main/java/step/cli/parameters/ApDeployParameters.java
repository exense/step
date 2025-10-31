package step.cli.parameters;

public class ApDeployParameters extends ApParameters<ApDeployParameters> {
    private Boolean async;
    private String apVersion;
    private String activationExpression;
    private Boolean forceRefreshOfSnapshots;

    public Boolean getAsync() {
        return async;
    }

    public ApDeployParameters setAsync(Boolean async) {
        this.async = async;
        return this;
    }

    public String getApVersion() {
        return apVersion;
    }

    public ApDeployParameters setApVersion(String apVersion) {
        this.apVersion = apVersion;
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
