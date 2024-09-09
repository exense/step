package step.core.agents.provisioning;

import java.util.List;

public class AgentProvisioningLog {

    public List<String> events;
    public List<String> logs;

    @Override
    public String toString() {
        return "AgentProvisioningLog{" +
                "events=" + events +
                ", logs=" + logs +
                '}';
    }
}
