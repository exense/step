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
 * This class represents a configured agent pool template that can be referenced in {@link AgentPoolRequirementSpec}
 */
public class AgentPoolSpec {

    /**
     * The local name within its factory
     */
    public String localName;
    /**
     * The global name across all factories
     */
    public String name;
    /**
     * The name that will be displayed in the UI
     */
    public String displayName;
    public Map<String, String> attributes;
    public int numberOfTokens;
    /**
     * The unique name of the agent pool factory corresponding to the factoryUrl
     */
    public String factoryName;
    /**
     * The base Url of the agent pool factory to be used to provision agent pools using this template
     */
    public String factoryUrl;
    public Set<AgentPoolProvisioningParameter> supportedProvisioningParameters;
    public int factoryAgenStartTimeoutSeconds;

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
        this.localName = name;
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
                ", factoryUrl='" + factoryUrl + '\'' +
                ", factoryAgenStartTimeoutSeconds=" + factoryAgenStartTimeoutSeconds +
                '}';
    }
}
