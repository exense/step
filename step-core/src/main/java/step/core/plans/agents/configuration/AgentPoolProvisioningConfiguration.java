package step.core.plans.agents.configuration;

import java.util.Objects;

public class AgentPoolProvisioningConfiguration {

    public int replicas;
    public String pool;
    public String image;

    public AgentPoolProvisioningConfiguration() {
    }

    public AgentPoolProvisioningConfiguration(String pool, String image, int replicas) {
        this.replicas = replicas;
        this.pool = pool;
        this.image = image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentPoolProvisioningConfiguration that = (AgentPoolProvisioningConfiguration) o;
        return replicas == that.replicas && Objects.equals(pool, that.pool) && Objects.equals(image, that.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(replicas, pool, image);
    }
}
