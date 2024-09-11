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

package step.artefacts.handlers.functions;

import step.core.agents.provisioning.AgentPoolProvisioningParameters;
import step.grid.TokenPretender;
import step.grid.tokenpool.Identity;
import step.grid.tokenpool.Interest;
import step.grid.tokenpool.SimpleAffinityEvaluator;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This {@link step.grid.tokenpool.AffinityEvaluator} is used to match agent token pools before their provisioning.
 * The reason for this is that some attributes are only available after provisioning. An example is the attribute
 * $dockerImage which is only populated after provisioning of the agent pool
 */
public class PreProvisioningTokenAffinityEvaluator extends SimpleAffinityEvaluator {
    @Override
    public int getAffinityScore(Identity i1, Identity i2) {
        return super.getAffinityScore(replaceCriteria(i1), replaceCriteria(i2));
    }

    private static TokenPretender replaceCriteria(Identity i1) {
        // Delegate the transformation of the selection criteria to the registered agent pool provisioning parameters. The first non-null transformation is returned
        Map<String, Interest> newInterests = i1.getInterests().entrySet().stream().map(e -> AgentPoolProvisioningParameters.supportedParameters.stream()
                .map(p -> p.preProvisioningTokenSelectionCriteriaTransformer.apply(e)).filter(Objects::nonNull).findFirst().orElse(e)).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        TokenPretender changedI1 = new TokenPretender(i1.getAttributes(), newInterests);
        return changedI1;
    }
}
