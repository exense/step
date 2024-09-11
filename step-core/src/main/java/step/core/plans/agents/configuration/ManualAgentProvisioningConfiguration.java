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

package step.core.plans.agents.configuration;

import step.core.agents.provisioning.AgentPoolProvisioningParameters;
import step.core.agents.provisioning.AgentPoolRequirementSpec;
import step.core.yaml.YamlModel;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.jsonschema.JsonSchema;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//Not scanned for now
@YamlModel(name = ManualAgentProvisioningConfiguration.AGENT_CONFIGURATION_YAML_NAME)
public class ManualAgentProvisioningConfiguration implements AgentProvisioningConfiguration {

    public static final String AGENT_CONFIGURATION_YAML_NAME = "agents";
    public static final String AGENT_POOL_CONFIGURATION_ARRAY_DEF = "agentPoolConfigurationArrayDef";

    @JsonSchema(ref = YamlJsonSchemaHelper.DEFS_PREFIX + AGENT_POOL_CONFIGURATION_ARRAY_DEF)
    public List<AgentPoolProvisioningConfiguration> configuredAgentPools;

    public List<AgentPoolRequirementSpec> getAgentPoolRequirementSpecs() {
        if (configuredAgentPools != null) {
            return configuredAgentPools.stream().map(p -> new AgentPoolRequirementSpec(p.pool, p.image != null ? Map.of(AgentPoolProvisioningParameters.PROVISIONING_PARAMETER_DOCKER_IMAGE, p.image) : Map.of(), p.replicas)).collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    @Override
    public boolean enableAutomaticTokenNumberCalculation() {
        return false;
    }

    @Override
    public boolean enableAgentProvisioning() {
        return configuredAgentPools != null && !configuredAgentPools.isEmpty();
    }
}
