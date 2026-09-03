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

import org.junit.Assert;
import org.junit.Test;
import step.core.agents.AgentTypeConstants;
import step.core.agents.provisioning.AgentPoolRequirementSpec;
import step.core.execution.ProvisioningException;
import step.functions.Function;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.grid.agent.AgentTypes;
import step.grid.tokenpool.Interest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Covers what a plan configuring its agent pools manually is provisioned with locally: the pool names it gives belong
 * to another Step instance, so the agents are derived from the keywords instead.
 */
public class LocalAgentPoolRequirementsTest {

    private static final Set<String> AVAILABLE_AGENT_TYPES =
        Set.of(AgentTypeConstants.AGENT_TYPE_JAVA, AgentTypeConstants.AGENT_TYPE_NODEJS);
    private static final String JAVA_POOL = "local-" + AgentTypeConstants.AGENT_TYPE_JAVA;
    private static final String NODE_POOL = "local-" + AgentTypeConstants.AGENT_TYPE_NODEJS;
    private static final java.util.function.Function<String, String> INSTALLATION_HINTS =
        agentType -> AgentTypeConstants.AGENT_TYPE_DOTNET.equals(agentType) ? "Install the .NET agent." : null;

    @Test
    public void startsOneAgentPerRequiredAgentType() {
        List<AgentPoolRequirementSpec> requirements = forRequiredAgentTypes(
            List.of(function(AgentTypeConstants.AGENT_TYPE_JAVA), function(AgentTypeConstants.AGENT_TYPE_NODEJS)), 5);

        Assert.assertEquals(Set.of(JAVA_POOL, NODE_POOL), poolNames(requirements));
        requirements.forEach(r -> Assert.assertEquals(5, r.numberOfAgents));
    }

    /**
     * A plan which disabled the automatic sizing gets everything a local agent may provide, hence one requirement of
     * as many single token agents as the maximum allows.
     */
    @Test
    public void sizesEachAgentWithTheMaximumNumberOfTokens() {
        List<AgentPoolRequirementSpec> requirements =
            forRequiredAgentTypes(List.of(function(AgentTypeConstants.AGENT_TYPE_JAVA)), 3);

        Assert.assertEquals(1, requirements.size());
        Assert.assertEquals(JAVA_POOL, requirements.get(0).agentPoolTemplateName);
        Assert.assertEquals(3, requirements.get(0).numberOfAgents);
    }

    /**
     * Keywords running in the engine itself, composite keywords typically, need no agent of their own.
     */
    @Test
    public void ignoresTheKeywordsRunningInTheEngine() {
        Function localFunction = function(AgentTypeConstants.AGENT_TYPE_JAVA);
        localFunction.setExecuteLocally(true);

        List<AgentPoolRequirementSpec> requirements =
            forRequiredAgentTypes(List.of(localFunction, function(AgentTypeConstants.AGENT_TYPE_NODEJS)), 5);

        Assert.assertEquals(Set.of(NODE_POOL), poolNames(requirements));
    }

    /**
     * A plan whose keywords all run in the engine needs no agent at all, and starting any would only cost the time of
     * starting it.
     */
    @Test
    public void startsNoAgentWhenNoKeywordRequiresOne() {
        Assert.assertEquals(List.of(), forRequiredAgentTypes(List.of(), 5));
    }

    @Test
    public void failsWhenARequiredAgentTypeIsNotAvailable() {
        ProvisioningException exception = Assert.assertThrows(ProvisioningException.class, () -> forRequiredAgentTypes(
            List.of(function(AgentTypeConstants.AGENT_TYPE_JAVA), function(AgentTypeConstants.AGENT_TYPE_DOTNET)), 5));

        Assert.assertEquals("This plan requires agent types which are not available for local execution: "
            + AgentTypeConstants.AGENT_TYPE_DOTNET + ". Install the .NET agent.", exception.getMessage());
    }

    @Test
    public void failsWhenTheTypeOfAKeywordIsUnknown() {
        ProvisioningException exception = Assert.assertThrows(ProvisioningException.class,
            () -> forRequiredAgentTypes(List.of(function(null)), 5));

        Assert.assertEquals("Unable to determine the agent required by the keyword keywordWithoutType: its type is"
            + " not supported by this local execution.", exception.getMessage());
    }

    @Test
    public void failsWhenTheTypeOfAKeywordDeclaresNoAgentType() {
        Function function = function(AgentTypeConstants.AGENT_TYPE_JAVA);

        ProvisioningException exception = Assert.assertThrows(ProvisioningException.class,
            () -> LocalAgentPoolRequirements.forRequiredAgentTypes(List.of(function),
                new FunctionTypeRegistryStub(false), AVAILABLE_AGENT_TYPES, 5, INSTALLATION_HINTS));

        Assert.assertEquals("Unable to determine the agent required by the keyword keywordFor"
            + AgentTypeConstants.AGENT_TYPE_JAVA + ": its type declares no agent type.", exception.getMessage());
    }

    private static List<AgentPoolRequirementSpec> forRequiredAgentTypes(List<Function> functions, int tokensPerAgent) {
        return LocalAgentPoolRequirements.forRequiredAgentTypes(functions, new FunctionTypeRegistryStub(true),
            AVAILABLE_AGENT_TYPES, tokensPerAgent, INSTALLATION_HINTS);
    }

    private static Set<String> poolNames(List<AgentPoolRequirementSpec> requirements) {
        return requirements.stream().map(r -> r.agentPoolTemplateName).collect(Collectors.toSet());
    }

    /**
     * @return a keyword whose function type requires the given agent type, which is how a real function type declares
     * the agent its keywords need (see {@link AbstractFunctionType#getTokenSelectionCriteria}), or a keyword the
     * registry has no type for when it is {@code null}
     */
    private static Function function(String agentType) {
        Function function = new Function();
        function.addAttribute("name", agentType == null ? "keywordWithoutType" : "keywordFor" + agentType);
        function.addAttribute("agentType", agentType);
        return function;
    }

    private static class FunctionTypeRegistryStub implements FunctionTypeRegistry {

        private final boolean declaresTheAgentType;

        private FunctionTypeRegistryStub(boolean declaresTheAgentType) {
            this.declaresTheAgentType = declaresTheAgentType;
        }

        @Override
        public AbstractFunctionType<Function> getFunctionType(String functionType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AbstractFunctionType<Function> getFunctionTypeByFunction(Function function) {
            if (function.getAttribute("agentType") == null) {
                // As a registry does for a keyword of a type this application does not have
                throw new RuntimeException("Unsupported function type");
            }
            return new AbstractFunctionType<>() {
                @Override
                public Map<String, Interest> getTokenSelectionCriteria(Function f) {
                    Map<String, Interest> criteria = new HashMap<>();
                    if (declaresTheAgentType) {
                        criteria.put(AgentTypes.AGENT_TYPE_KEY,
                            new Interest(Pattern.compile(f.getAttribute("agentType")), true));
                    }
                    return criteria;
                }

                @Override
                public String getHandlerChain(Function f) {
                    return "aHandler";
                }

                @Override
                public Function newFunction() {
                    return new Function();
                }
            };
        }

        @Override
        public void registerFunctionType(AbstractFunctionType<? extends Function> functionType) {
            throw new UnsupportedOperationException();
        }
    }
}
