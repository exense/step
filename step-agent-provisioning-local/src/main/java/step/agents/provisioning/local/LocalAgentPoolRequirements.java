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

import step.core.accessors.AbstractOrganizableObject;
import step.core.agents.provisioning.AgentPoolRequirementSpec;
import step.core.execution.ProvisioningException;
import step.functions.Function;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.grid.agent.AgentTypes;
import step.grid.tokenpool.Interest;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private LocalAgentPoolRequirements() {
    }

    /**
     * @param functions            the keywords available to the execution, used to determine which agent types it needs
     * @param functionTypeRegistry the registry resolving the type, and thus the required agent, of a keyword
     * @param availableAgentTypes  the agent types which can be started on this machine
     * @param tokensPerAgent       the number of tokens to give each started agent
     * @param installationHints    what the user has to do for an agent type to become available, by agent type
     * @return one requirement per required agent type, empty when no keyword needs an agent of its own
     * @throws ProvisioningException when a keyword requires an agent this machine cannot start, or one whose agent
     * type cannot be determined
     */
    static List<AgentPoolRequirementSpec> forRequiredAgentTypes(Collection<Function> functions,
                                                               FunctionTypeRegistry functionTypeRegistry,
                                                               Set<String> availableAgentTypes, int tokensPerAgent,
                                                               java.util.function.Function<String, String> installationHints) {
        Set<String> requiredAgentTypes = requiredAgentTypes(functions, functionTypeRegistry);
        Set<String> unavailableAgentTypes = new HashSet<>(requiredAgentTypes);
        unavailableAgentTypes.removeAll(availableAgentTypes);
        if (!unavailableAgentTypes.isEmpty()) {
            throw new ProvisioningException(unavailableAgentTypesMessage(unavailableAgentTypes, installationHints));
        }
        return requiredAgentTypes.stream()
            .map(agentType -> new AgentPoolRequirementSpec(LocalProcessAgentProvisioningDriver.agentPoolName(agentType),
                tokensPerAgent))
            .collect(Collectors.toList());
    }

    /**
     * @return the message reporting agent types no local agent can serve, telling for each one what the user has to
     * do about it when its provider knows
     */
    static String unavailableAgentTypesMessage(Collection<String> agentTypes,
                                               java.util.function.Function<String, String> installationHints) {
        StringBuilder message = new StringBuilder("This plan requires agent types which are not available for local"
            + " execution: " + String.join(", ", agentTypes) + ".");
        agentTypes.stream().map(installationHints).filter(Objects::nonNull)
            .forEach(hint -> message.append(" ").append(hint));
        return message.toString();
    }

    /**
     * @return the agent types the given keywords need, as their function types declare them. Keywords running in the
     * engine itself, composite keywords being the usual case, need no agent and are left out.
     */
    private static Set<String> requiredAgentTypes(Collection<Function> functions, FunctionTypeRegistry functionTypeRegistry) {
        Set<String> agentTypes = new HashSet<>();
        for (Function function : functions) {
            if (!function.requiresLocalExecution()) {
                agentTypes.add(agentTypeOf(function, functionTypeRegistry));
            }
        }
        return agentTypes;
    }

    /**
     * @return the agent type the given keyword declares through its function type
     * @throws ProvisioningException when the type of the keyword is unknown to this application or declares no agent
     * type: the keyword would be provisioned no agent and fail when called
     */
    private static String agentTypeOf(Function function, FunctionTypeRegistry functionTypeRegistry) {
        Map<String, Interest> criteria;
        try {
            AbstractFunctionType<Function> functionType = functionTypeRegistry.getFunctionTypeByFunction(function);
            criteria = functionType.getTokenSelectionCriteria(function);
        } catch (RuntimeException e) {
            throw new ProvisioningException("Unable to determine the agent required by the keyword "
                + nameOf(function) + ": its type is not supported by this local execution.", e);
        }
        Interest agentType = criteria != null ? criteria.get(AgentTypes.AGENT_TYPE_KEY) : null;
        if (agentType == null || agentType.getSelectionPattern() == null) {
            throw new ProvisioningException("Unable to determine the agent required by the keyword "
                + nameOf(function) + ": its type declares no agent type.");
        }
        return agentType.getSelectionPattern().pattern();
    }

    private static String nameOf(Function function) {
        return function.getAttribute(AbstractOrganizableObject.NAME);
    }
}
