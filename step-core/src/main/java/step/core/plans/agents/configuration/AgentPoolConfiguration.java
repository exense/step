package step.core.plans.agents.configuration;

import java.util.Objects;

/**
 * Defines the requirements in terms of Agents for a Plan
 */
public class AgentPoolConfiguration {
    public static String TEMPLATE_PROPERTY_NAME = "pool";
    public static String NUMBER_AGENT_PROPERTY_NAME = "replicas";
    public static String IMAGE_PROPERTY_NAME = "image";

    public int replicas;
    public String pool;
    public String image;

    public AgentPoolConfiguration() {
    }


    public AgentPoolConfiguration(String pool, String image, int replicas) {
        this.replicas = replicas;
        this.pool = pool;
        this.image = image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentPoolConfiguration that = (AgentPoolConfiguration) o;
        return replicas == that.replicas && Objects.equals(pool, that.pool) && Objects.equals(image, that.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(replicas, pool, image);
    }
}
