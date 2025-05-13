package step.core.agents.provisioning.driver;

import org.bson.types.ObjectId;
import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.core.collections.Filters;

import java.util.Objects;

public class AgentProvisioningStatusAccessor extends AbstractAccessor<AgentProvisioningStatus> {

    public AgentProvisioningStatusAccessor(Collection<AgentProvisioningStatus> collectionDriver) {
        super(collectionDriver);
    }

    public void removeAgentProvisioningStatusByExecutionID(ObjectId executionID) {
        Objects.requireNonNull(executionID);
        collectionDriver.remove(Filters.equals("executionId", executionID.toHexString()));
    }
}
