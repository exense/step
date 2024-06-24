package step.artefacts.handlers.functions.autoscaler;

import java.util.Objects;

public class TemplateStsAgentPoolRequirementSpec extends AgentPoolRequirementSpec {

    public String templateStatefulSetName;
    public String dockerImage;

    public TemplateStsAgentPoolRequirementSpec() {
    }

    public TemplateStsAgentPoolRequirementSpec(String templateStatefulSetName, int numberOfAgents) {
        this.templateStatefulSetName = templateStatefulSetName;
        this.numberOfAgents = numberOfAgents;
    }

    public TemplateStsAgentPoolRequirementSpec(String templateStatefulSetName, String dockerImage, int numberOfAgents) {
        this.templateStatefulSetName = templateStatefulSetName;
        this.dockerImage = dockerImage;
        this.numberOfAgents = numberOfAgents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateStsAgentPoolRequirementSpec that = (TemplateStsAgentPoolRequirementSpec) o;
        return Objects.equals(templateStatefulSetName, that.templateStatefulSetName) && Objects.equals(dockerImage, that.dockerImage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateStatefulSetName, dockerImage);
    }
}
