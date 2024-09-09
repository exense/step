package step.artefacts.handlers.functions.autoscaler;

import java.util.Map;

public class TokenProvisioningStatus {

    public String statusDescription;
    public boolean completed = false;

    public TokenProvisioningError error;
    public AgentProvisioningLogs provisioningLogs;
}
