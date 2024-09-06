package step.artefacts.handlers.functions.autoscaler;

import step.core.agents.provisioning.AgentProvisioningLogs;

public class TokenProvisioningStatus {

    public String statusDescription;
    public boolean completed = false;

    public TokenProvisioningError error;
    public AgentProvisioningLogs provisioningLogs;
}
