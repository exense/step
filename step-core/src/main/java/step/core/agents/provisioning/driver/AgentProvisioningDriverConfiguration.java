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

package step.core.agents.provisioning.driver;

import step.core.agents.provisioning.AgentPoolSpec;

import java.util.Set;

public class AgentProvisioningDriverConfiguration {

    public Set<AgentPoolSpec> availableAgentPools;

    public AgentProvisioningDriverConfiguration() {}

    public AgentProvisioningDriverConfiguration(Set<AgentPoolSpec> availableAgentPools) {
        this.availableAgentPools = availableAgentPools;
    }
}