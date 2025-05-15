package step.core.agents.provisioning;

/**
 * Class containing restrictions for the provisioning.
 * This is meant for situations where licensing is enabled,
 * and the number of concurrent agents and/or tokens that
 * the controller accepts might be limited.
 *
 * Limits are independent of each other, and if a limit is
 * present (not null), the controller will not handle
 * registration requests beyond the limit, instead ignoring
 * the requests.
 *
 * It is therefore advised to either only provision accordingly
 * to stay within the stated limits, or to reject a request
 * altogether if any of the limits would be exceeded.
 */
public class AgentProvisioningRestrictions {
    public Integer agentCountLimit;
    public Integer tokenCountLimit;

    @Override
    public String toString() {
        return "AgentProvisioningRestrictions{" +
                "agentCountLimit=" + agentCountLimit +
                ", tokenCountLimit=" + tokenCountLimit +
                '}';
    }
}
