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

import java.util.Objects;

public class AgentPoolProvisioningConfiguration {

    public int replicas;
    public String pool;
    public String image;

    public AgentPoolProvisioningConfiguration() {
    }

    public AgentPoolProvisioningConfiguration(String pool, String image, int replicas) {
        this.replicas = replicas;
        this.pool = pool;
        this.image = image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentPoolProvisioningConfiguration that = (AgentPoolProvisioningConfiguration) o;
        return replicas == that.replicas && Objects.equals(pool, that.pool) && Objects.equals(image, that.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(replicas, pool, image);
    }
}
