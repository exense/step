package step.artefacts.handlers.functions.autoscaler;

import java.util.Objects;

public class CustomAgentPoolRequirementSpec extends AgentPoolRequirementSpec {

    public String dockerImage;

    public CustomAgentPoolRequirementSpec() {
    }

    public CustomAgentPoolRequirementSpec(String dockerImage, int numberOfAgents) {
        this.dockerImage = dockerImage;
        this.numberOfAgents = numberOfAgents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomAgentPoolRequirementSpec that = (CustomAgentPoolRequirementSpec) o;
        return Objects.equals(dockerImage, that.dockerImage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dockerImage);
    }
}
