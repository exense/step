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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import step.core.agents.provisioning.AgentPoolRequirementSpec;

import java.util.List;

@JsonSubTypes({
        @JsonSubTypes.Type(value = AutomaticAgentProvisioningConfiguration.class),
        @JsonSubTypes.Type(value = ManualAgentProvisioningConfiguration.class)
})
public interface AgentProvisioningConfiguration {

    boolean enableAutomaticTokenNumberCalculation();

    boolean enableAgentProvisioning();

    @JsonIgnore
    List<AgentPoolRequirementSpec> getAgentPoolRequirementSpecs();

}