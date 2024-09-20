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

import step.grid.tokenpool.Interest;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AgentPoolProvisioningParameters {

    public static final String TOKEN_ATTRIBUTE_DOCKER_IMAGE = "$dockerImage";
    public static final String TOKEN_ATTRIBUTE_DOCKER_SUPPORT = "$supportsCustomDockerImage";

    public static final String PROVISIONING_PARAMETER_DOCKER_IMAGE = "dockerImage";
    public static final AgentPoolProvisioningParameter DOCKER_IMAGE = new AgentPoolProvisioningParameter(PROVISIONING_PARAMETER_DOCKER_IMAGE, "Docker image", (criteria, provisioningParameters) -> {
        if(criteria.containsKey(TOKEN_ATTRIBUTE_DOCKER_IMAGE)) {
            provisioningParameters.put(PROVISIONING_PARAMETER_DOCKER_IMAGE, criteria.get(TOKEN_ATTRIBUTE_DOCKER_IMAGE).getSelectionPattern().pattern());
        }
    }, criteria -> {
        if (criteria.getKey().equals(TOKEN_ATTRIBUTE_DOCKER_IMAGE)) {
            return Map.entry(TOKEN_ATTRIBUTE_DOCKER_SUPPORT, new Interest(Pattern.compile(Boolean.TRUE.toString()), true));
        } else {
            return null;
        }
    });

    public static final List<AgentPoolProvisioningParameter> supportedParameters = List.of(DOCKER_IMAGE);
}
