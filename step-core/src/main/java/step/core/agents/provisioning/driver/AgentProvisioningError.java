package step.core.agents.provisioning.driver;

public class AgentProvisioningError {

    public String errorMessage;
    // Empty constructor needed for serialization
    public AgentProvisioningError() {}
    public AgentProvisioningError(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
