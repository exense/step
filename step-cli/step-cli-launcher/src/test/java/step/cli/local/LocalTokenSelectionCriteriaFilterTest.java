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
package step.cli.local;

import org.junit.Assert;
import org.junit.Test;
import step.core.agents.AgentTypeConstants;
import step.grid.agent.AgentTypes;
import step.grid.tokenpool.Interest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static step.core.agents.provisioning.AgentPoolConstants.TOKEN_ATTRIBUTE_PARTITION;

public class LocalTokenSelectionCriteriaFilterTest {

    /**
     * The agent type decides which of the started agents runs the keyword and is therefore the one criterion a local
     * execution can honour, together with the partition isolating the tokens of the execution.
     */
    @Test
    public void keepsTheCriteriaALocalExecutionCanHonour() {
        Map<String, Interest> criteria = criteria(
            AgentTypes.AGENT_TYPE_KEY, AgentTypeConstants.AGENT_TYPE_NODEJS,
            TOKEN_ATTRIBUTE_PARTITION, "anExecution");

        Assert.assertEquals(criteria, new LocalTokenSelectionCriteriaFilter().filter(criteria));
    }

    /**
     * Routing criteria describe the agents of an infrastructure which does not exist here. Keeping them would match no
     * token at all, so the keyword is routed by agent type alone.
     */
    @Test
    public void dropsTheRoutingCriteria() {
        Map<String, Interest> criteria = criteria(
            AgentTypes.AGENT_TYPE_KEY, AgentTypeConstants.AGENT_TYPE_JAVA,
            "OS", "WINDOWS",
            "$dockerImage", "an-image:1.0");

        Map<String, Interest> filtered = new LocalTokenSelectionCriteriaFilter().filter(criteria);

        Assert.assertEquals(Set.of(AgentTypes.AGENT_TYPE_KEY), filtered.keySet());
        Assert.assertEquals(AgentTypeConstants.AGENT_TYPE_JAVA,
            filtered.get(AgentTypes.AGENT_TYPE_KEY).getSelectionPattern().pattern());
    }

    @Test
    public void leavesTheGivenCriteriaUntouched() {
        Map<String, Interest> criteria = criteria(AgentTypes.AGENT_TYPE_KEY, AgentTypeConstants.AGENT_TYPE_JAVA,
            "OS", "WINDOWS");

        new LocalTokenSelectionCriteriaFilter().filter(criteria);

        Assert.assertEquals(2, criteria.size());
    }

    /**
     * @param keysAndPatterns criterion name and selection pattern, in pairs
     */
    private static Map<String, Interest> criteria(String... keysAndPatterns) {
        Map<String, Interest> criteria = new LinkedHashMap<>();
        for (int i = 0; i < keysAndPatterns.length; i += 2) {
            criteria.put(keysAndPatterns[i], new Interest(Pattern.compile(keysAndPatterns[i + 1]), true));
        }
        return criteria;
    }
}
