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

/**
 * Starts the agent of one specific {@link step.core.agents.AgentTypeConstants agent type} as a separate process on the local
 * machine. Implementations are passed to {@link LocalProcessAgentProvisioningDriver}, which offers an agent type for
 * local execution if and only if its provider reports itself as {@link #isAvailable() available}.
 * <p>
 * None of the agents bring their own runtime: the Java agent runs on a JVM, the Node.js agent on Node.js and the .NET
 * agent on .NET. Only the Java one is therefore available by construction, the application starting it being itself a
 * Java application; the others depend on what the developer machine happens to provide. Deciding that, and doing it
 * without starting anything, is what {@link #isAvailable()} is for.
 */
public interface LocalAgentProvider {

    /**
     * @return the agent type this provider starts, i.e. one of the constants of {@link step.core.agents.AgentTypeConstants}.
     * It is declared on the tokens of the started agent and is what a keyword is routed by.
     */
    String getAgentType();

    /**
     * @return a human readable name of this agent type, used in logs and provisioning reports
     */
    String getDisplayName();

    /**
     * Reports whether this provider is able to start its agent on this machine, typically by checking that the agent
     * is available to this CLI and that the runtime it needs is installed. A provider which is not available is
     * silently left out of the agent pools offered for local execution, causing keywords requiring it to fail with a
     * "no matching agent pool" error rather than with an obscure process start failure.
     */
    boolean isAvailable();

    /**
     * @return what a user has to do for this agent type to become {@link #isAvailable() available} on this machine, or
     * {@code null} when there is nothing helpful to say. It is appended to the error of an execution requiring an
     * agent type no provider offers, which is otherwise only able to name the type it could not route to.
     */
    default String getInstallationHint() {
        return null;
    }

    /**
     * Starts the agent process. The returned handle is always started, never partially initialized: implementations
     * which fail half way have to clean up whatever they created before throwing.
     * <p>
     * This method returns as soon as the process has been started. Waiting for the agent to actually register to the
     * grid is the responsibility of the caller, which is the only one knowing how many tokens it expects.
     */
    LocalAgentProcess start(LocalAgentStartContext context) throws LocalAgentException;
}
