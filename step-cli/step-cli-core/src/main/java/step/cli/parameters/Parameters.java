package step.cli.parameters;

public abstract class Parameters<T extends Parameters<T>> {
    private String stepProjectName;
    private String authToken;

    public String getStepProjectName() {
        return stepProjectName;
    }

    public T setStepProjectName(String stepProjectName) {
        this.stepProjectName = stepProjectName;
        //noinspection unchecked
        return (T) this;
    }

    public String getAuthToken() {
        return authToken;
    }

    public T setAuthToken(String authToken) {
        this.authToken = authToken;
        //noinspection unchecked
        return (T) this;
    }


}