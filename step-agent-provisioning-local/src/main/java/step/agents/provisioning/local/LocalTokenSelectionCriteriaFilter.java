/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.agents.provisioning.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.handlers.functions.TokenSelectionCriteriaFilter;
import step.grid.agent.AgentTypes;
import step.grid.tokenpool.Interest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static step.core.agents.provisioning.AgentPoolConstants.TOKEN_ATTRIBUTE_PARTITION;

/**
 * Reduces the token selection criteria of a keyword to what a local execution can actually decide.
 * <p>
 * Keywords and plans can be routed to specific agents with arbitrary criteria: an operating system, a region, a
 * docker image, or any attribute the agents of a real infrastructure were configured with. None of that exists here:
 * a local execution has one machine, and the agents running on it are the ones this CLI just started, whose only
 * distinguishing attribute is the type of keyword they run. Keeping the criteria would not route anything, it would
 * simply match no token: the forecasting would report that no agent pool is available and the execution would then
 * time out waiting for a token which cannot exist.
 * <p>
 * The agent type is therefore kept - it decides which of the started agents runs the keyword, which is a real
 * decision - and everything else is dropped and reported.
 */
public class LocalTokenSelectionCriteriaFilter implements TokenSelectionCriteriaFilter {

    private static final Logger logger = LoggerFactory.getLogger(LocalTokenSelectionCriteriaFilter.class);

    /**
     * The criteria a local execution can honour: the type of agent the keyword needs, and the partition isolating the
     * tokens of this execution.
     */
    private static final Set<String> SUPPORTED_CRITERIA = Set.of(AgentTypes.AGENT_TYPE_KEY, TOKEN_ATTRIBUTE_PARTITION);

    /**
     * The criteria already reported, so that a keyword called in a loop doesn't report the same thing at every
     * iteration. Kept per execution, this filter being registered per execution context.
     */
    private final Set<Map<String, String>> reportedCriteria = ConcurrentHashMap.newKeySet();

    @Override
    public Map<String, Interest> filter(Map<String, Interest> selectionCriteria) {
        Map<String, Interest> supported = new LinkedHashMap<>();
        // Sorted, so that the same set of dropped criteria is reported once whatever the order they were collected in
        Map<String, String> dropped = new TreeMap<>();
        selectionCriteria.forEach((criterion, interest) -> {
            if (SUPPORTED_CRITERIA.contains(criterion)) {
                supported.put(criterion, interest);
            } else {
                String patternStr = "unknown";
                if (interest != null) {
                    Pattern pattern = interest.getSelectionPattern();
                    if (pattern != null) {
                        patternStr = pattern.pattern();
                    } else {
                        patternStr = interest.toString();
                    }
                }
                dropped.put(criterion, patternStr);
            }
        });

        if (!dropped.isEmpty() && reportedCriteria.add(dropped)) {
            Interest agentType = supported.get(AgentTypes.AGENT_TYPE_KEY);
            logger.warn("The routing criteria {} are ignored: a local execution runs all keywords on this machine, " +
                    "on the agents started for it. The keywords concerned are routed by agent type only ({}).",
                dropped, agentType != null ? agentType.getSelectionPattern().pattern() : "none requested");
        }
        return supported;
    }
}
