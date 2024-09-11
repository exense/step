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
import java.util.Set;

/**
 * This class represents a configured agent pool that can be referenced in {@link AgentPoolRequirementSpec}
 */
public class AgentPoolSpec {

    public String name;
    public String displayName;
    public Map<String, String> attributes;
    public int numberOfTokens;
    public Set<AgentPoolProvisioningParameter> supportedProvisioningParameters;

    public AgentPoolSpec() {
    }

    public AgentPoolSpec(String name, Map<String, String> attributes, int numberOfTokens) {
        this(name, attributes, numberOfTokens, Set.of());
    }

    public AgentPoolSpec(String name, Map<String, String> attributes, int numberOfTokens, Set<AgentPoolProvisioningParameter> supportedProvisioningParameters) {
        this(name, name, attributes, numberOfTokens, supportedProvisioningParameters);
    }

    public AgentPoolSpec(String name, String displayName, Map<String, String> attributes, int numberOfTokens, Set<AgentPoolProvisioningParameter> supportedProvisioningParameters) {
        this.name = name;
        this.displayName = displayName;
        this.attributes = attributes;
        this.numberOfTokens = numberOfTokens;
        this.supportedProvisioningParameters = supportedProvisioningParameters;
    }

    @Override
    public String toString() {
        return "AgentPoolSpec{" +
                "name='" + name + '\'' +
                ", attributes=" + attributes +
                ", numberOfTokens=" + numberOfTokens +
                '}';
    }
}
