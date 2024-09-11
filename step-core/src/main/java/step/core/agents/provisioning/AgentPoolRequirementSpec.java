/*
 * Copyright (C) 2024, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

package step.core.agents.provisioning;

import java.util.Map;
import java.util.Objects;

/**
 * Defines the agent pool provisioning requirements
 */
public class AgentPoolRequirementSpec {
    public int numberOfAgents;
    public String agentPoolTemplateName;
    public Map<String, String> provisioningParameters;

    public AgentPoolRequirementSpec() {
    }

    public AgentPoolRequirementSpec(String agentPoolTemplateName, int numberOfAgents) {
        this(agentPoolTemplateName, Map.of(), numberOfAgents);
    }
    public AgentPoolRequirementSpec(String agentPoolTemplateName, Map<String, String> provisioningParameters, int numberOfAgents) {
        this.numberOfAgents = numberOfAgents;
        this.agentPoolTemplateName = agentPoolTemplateName;
        this.provisioningParameters = provisioningParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentPoolRequirementSpec that = (AgentPoolRequirementSpec) o;
        return numberOfAgents == that.numberOfAgents && Objects.equals(agentPoolTemplateName, that.agentPoolTemplateName) && Objects.equals(provisioningParameters, that.provisioningParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfAgents, agentPoolTemplateName, provisioningParameters);
    }

    @Override
    public String toString() {
        return "AgentPoolRequirementSpec{" +
                "numberOfAgents=" + numberOfAgents +
                ", agentPoolTemplateName='" + agentPoolTemplateName + '\'' +
                ", provisioningParameters=" + provisioningParameters +
                '}';
    }
}
