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

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Covers the error a plan gets when it requires an agent type this machine cannot start. It is the whole output of a
 * non verbose run, hence the care: it has to name what is missing and what to do about it.
 */
public class UnavailableAgentsMessageTest {

    @Test
    public void namesTheAgentTypeAndWhatToDoAboutIt() {
        String message = LocalAgentProvisioningPlugin.unavailableAgentsMessage(
            Set.of(agentTypeCriteria(AgentTypeConstants.AGENT_TYPE_DOTNET)),
            agentType -> "Point --localAgentDotNet at an installed Step .NET agent.");

        Assert.assertEquals("This plan requires agent types which are not available for local execution: dotnet."
            + " Point --localAgentDotNet at an installed Step .NET agent.", message);
    }

    /**
     * A provider with nothing to add, or an agent type this distribution has no provider for at all.
     */
    @Test
    public void namesTheAgentTypeWhenThereIsNoHintForIt() {
        String message = LocalAgentProvisioningPlugin.unavailableAgentsMessage(
            Set.of(agentTypeCriteria("aTypeNobodyProvides")), agentType -> null);

        Assert.assertEquals("This plan requires agent types which are not available for local execution:"
            + " aTypeNobodyProvides.", message);
    }

    /**
     * Criteria this plugin cannot read as an agent type are still worth reporting: they are what the forecasting found
     * no pool for, and dropping them would leave the execution failing with nothing at all.
     */
    @Test
    public void fallsBackToTheCriteriaThemselves() {
        Map<String, Interest> criteria = Map.of("OS", new Interest(Pattern.compile("WINDOWS"), true));

        String message = LocalAgentProvisioningPlugin.unavailableAgentsMessage(Set.of(criteria), agentType -> null);

        Assert.assertTrue(message, message.startsWith("This plan requires agents which are not available for local execution: "));
        Assert.assertTrue(message, message.contains("OS"));
    }

    private static Map<String, Interest> agentTypeCriteria(String agentType) {
        return Map.of(AgentTypes.AGENT_TYPE_KEY, new Interest(Pattern.compile(agentType), true));
    }
}
