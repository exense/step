package step.core.agents.provisioning.driver;

import step.core.agents.provisioning.AgentProvisioningLogs;

public class AgentProvisioningStatus {

    public String statusDescription;
    public boolean completed = false;

    public AgentProvisioningError error;
    public AgentProvisioningLogs provisioningLogs;
}
