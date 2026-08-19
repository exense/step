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
import step.core.agents.provisioning.AgentPoolRequirementSpec;
import step.functions.Function;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.grid.agent.AgentTypes;
import step.grid.tokenpool.Interest;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the agent pool requirements of a local execution whose plan configures its agent pools manually.
 * <p>
 * A manual configuration names the pools of the Step instance the plan normally runs on ({@code windows-medium},
 * {@code linux-large}, ...). Those names mean nothing here and cannot be mapped: they describe machines of an
 * infrastructure, whereas a local execution has one machine and the agents this CLI can start on it. The number of
 * agents they ask for is just as meaningless, being a number of machines.
 * <p>
 * What the requirement is turned into is therefore one agent per keyword <i>type</i> the automation package needs,
 * each with as many tokens as a local agent is allowed to have: the plan asked not to be sized automatically, so it
 * is given everything the local execution can offer rather than a forecast it disabled.
 */
class LocalAgentPoolRequirements {

    private static final Logger logger = LoggerFactory.getLogger(LocalAgentPoolRequirements.class);

    private LocalAgentPoolRequirements() {
    }

    /**
     * @param functions          the keywords available to the execution, used to determine which agent types it needs
     * @param functionTypeRegistry the registry resolving the type, and thus the required agent, of a keyword
     * @param availableAgentTypes  the agent types which can be started on this machine
     * @param tokensPerAgent       the number of tokens to give each started agent
     * @return one requirement per required agent type, or one per available agent type when none could be determined
     */
    static List<AgentPoolRequirementSpec> forRequiredAgentTypes(Collection<Function> functions,
                                                               FunctionTypeRegistry functionTypeRegistry,
                                                               Set<String> availableAgentTypes, int tokensPerAgent) {
        Set<String> requiredAgentTypes = requiredAgentTypes(functions, functionTypeRegistry);
        requiredAgentTypes.retainAll(availableAgentTypes);
        if (requiredAgentTypes.isEmpty()) {
            // Either the keywords are unknown at this point, or none of them declares an agent type this machine can
            // serve. Starting everything available is the only answer left which lets the execution run at all.
            logger.debug("No agent type could be derived from the keywords. Starting one agent of each available type.");
            requiredAgentTypes = new LinkedHashSet<>(availableAgentTypes);
        }
        return requiredAgentTypes.stream()
            .map(agentType -> new AgentPoolRequirementSpec(LocalProcessAgentProvisioningDriver.agentPoolName(agentType),
                tokensPerAgent))
            .collect(Collectors.toList());
    }

    /**
     * @return the agent types the given keywords need, as their function types declare them. Keywords running in the
     * engine itself, composite keywords being the usual case, need no agent and are left out.
     */
    private static Set<String> requiredAgentTypes(Collection<Function> functions, FunctionTypeRegistry functionTypeRegistry) {
        Set<String> agentTypes = new LinkedHashSet<>();
        if (functions == null || functionTypeRegistry == null) {
            return agentTypes;
        }
        for (Function function : functions) {
            if (function.requiresLocalExecution()) {
                continue;
            }
            try {
                AbstractFunctionType<Function> functionType = functionTypeRegistry.getFunctionTypeByFunction(function);
                Map<String, Interest> criteria = functionType.getTokenSelectionCriteria(function);
                Interest agentType = criteria != null ? criteria.get(AgentTypes.AGENT_TYPE_KEY) : null;
                if (agentType != null) {
                    agentTypes.add(agentType.getSelectionPattern().pattern());
                }
            } catch (RuntimeException e) {
                // An unregistered function type is not worth failing the provisioning for: the keyword itself fails
                // with a far clearer error when it is called.
                logger.debug("Unable to determine the agent type of the keyword {}", function.getAttribute("name"), e);
            }
        }
        return agentTypes;
    }
}
