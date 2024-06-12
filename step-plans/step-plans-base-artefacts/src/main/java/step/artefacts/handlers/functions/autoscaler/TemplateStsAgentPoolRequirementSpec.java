package step.artefacts.handlers.functions.autoscaler;

import java.util.Objects;

public class TemplateStsAgentPoolRequirementSpec extends AgentPoolRequirementSpec {

    public TemplateStsAgentPoolRequirementSpec(String templateStatefulSetName, int numberOfAgents) {
        this.templateStatefulSetName = templateStatefulSetName;
        this.numberOfAgents = numberOfAgents;
    }

    public String templateStatefulSetName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateStsAgentPoolRequirementSpec that = (TemplateStsAgentPoolRequirementSpec) o;
        return Objects.equals(templateStatefulSetName, that.templateStatefulSetName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateStatefulSetName);
    }
}
