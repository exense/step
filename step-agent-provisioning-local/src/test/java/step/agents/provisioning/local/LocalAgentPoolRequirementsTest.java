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

    @Test
    public void startsOneAgentPerRequiredAgentType() {
        List<AgentPoolRequirementSpec> requirements = LocalAgentPoolRequirements.forRequiredAgentTypes(
            List.of(function(AgentTypeConstants.AGENT_TYPE_JAVA), function(AgentTypeConstants.AGENT_TYPE_NODEJS)),
            new FunctionTypeRegistryStub(), AVAILABLE_AGENT_TYPES, 5);

        Assert.assertEquals(Set.of(JAVA_POOL, NODE_POOL), poolNames(requirements));
        requirements.forEach(r -> Assert.assertEquals(5, r.numberOfAgents));
    }

    /**
     * A plan which disabled the automatic sizing gets everything a local agent may provide, hence one requirement of
     * as many single token agents as the maximum allows.
     */
    @Test
    public void sizesEachAgentWithTheMaximumNumberOfTokens() {
        List<AgentPoolRequirementSpec> requirements = LocalAgentPoolRequirements.forRequiredAgentTypes(
            List.of(function(AgentTypeConstants.AGENT_TYPE_JAVA)), new FunctionTypeRegistryStub(),
            AVAILABLE_AGENT_TYPES, 3);

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

        List<AgentPoolRequirementSpec> requirements = LocalAgentPoolRequirements.forRequiredAgentTypes(
            List.of(localFunction, function(AgentTypeConstants.AGENT_TYPE_NODEJS)), new FunctionTypeRegistryStub(),
            AVAILABLE_AGENT_TYPES, 5);

        Assert.assertEquals(Set.of(NODE_POOL), poolNames(requirements));
    }

    /**
     * An agent type this distribution cannot start is left out rather than requested and rejected later.
     */
    @Test
    public void ignoresTheAgentTypesWhichAreNotAvailable() {
        List<AgentPoolRequirementSpec> requirements = LocalAgentPoolRequirements.forRequiredAgentTypes(
            List.of(function(AgentTypeConstants.AGENT_TYPE_JAVA), function(AgentTypeConstants.AGENT_TYPE_DOTNET)),
            new FunctionTypeRegistryStub(), AVAILABLE_AGENT_TYPES, 5);

        Assert.assertEquals(Set.of(JAVA_POOL), poolNames(requirements));
    }

    /**
     * Without keywords to derive the types from, everything available is started: it is the only answer which lets the
     * execution run at all.
     */
    @Test
    public void startsEveryAvailableAgentTypeWhenNoneCanBeDerived() {
        List<AgentPoolRequirementSpec> requirements = LocalAgentPoolRequirements.forRequiredAgentTypes(
            List.of(), new FunctionTypeRegistryStub(), AVAILABLE_AGENT_TYPES, 5);

        Assert.assertEquals(Set.of(JAVA_POOL, NODE_POOL), poolNames(requirements));
    }

    @Test
    public void startsEveryAvailableAgentTypeWithoutAFunctionAccessor() {
        List<AgentPoolRequirementSpec> requirements = LocalAgentPoolRequirements.forRequiredAgentTypes(
            null, new FunctionTypeRegistryStub(), AVAILABLE_AGENT_TYPES, 5);

        Assert.assertEquals(Set.of(JAVA_POOL, NODE_POOL), poolNames(requirements));
    }

    private static Set<String> poolNames(List<AgentPoolRequirementSpec> requirements) {
        return requirements.stream().map(r -> r.agentPoolTemplateName).collect(Collectors.toSet());
    }

    /**
     * @return a keyword whose function type requires the given agent type, which is how a real function type declares
     * the agent its keywords need (see {@link AbstractFunctionType#getTokenSelectionCriteria})
     */
    private static Function function(String agentType) {
        Function function = new Function();
        function.addAttribute("name", "keywordFor" + agentType);
        function.addAttribute("agentType", agentType);
        return function;
    }

    private static class FunctionTypeRegistryStub implements FunctionTypeRegistry {

        @Override
        public AbstractFunctionType<Function> getFunctionType(String functionType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AbstractFunctionType<Function> getFunctionTypeByFunction(Function function) {
            return new AbstractFunctionType<>() {
                @Override
                public Map<String, Interest> getTokenSelectionCriteria(Function f) {
                    Map<String, Interest> criteria = new HashMap<>();
                    criteria.put(AgentTypes.AGENT_TYPE_KEY,
                        new Interest(Pattern.compile(f.getAttribute("agentType")), true));
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
