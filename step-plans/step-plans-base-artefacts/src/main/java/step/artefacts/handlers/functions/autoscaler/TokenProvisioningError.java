package step.artefacts.handlers.functions.autoscaler;

public class TokenProvisioningError {

    public String errorMessage;
    // Empty constructor needed for serialization
    public TokenProvisioningError() {}
    public TokenProvisioningError(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
