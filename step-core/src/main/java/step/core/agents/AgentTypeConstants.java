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

package step.core.agents;

import step.grid.agent.AgentTypes;

/**
 * The agent technologies Step supports. An agent declares its own under {@link AgentTypes#AGENT_TYPE_KEY} on the
 * tokens it emits, and a keyword is routed to a matching agent by the token selection criteria of its function type.
 */
public class AgentTypeConstants {

    /**
     * The type of the Java agent, the agent the grid ships itself. Named 'default' for historical reasons: it predates
     * the other types and is therefore also the type assumed when nothing else is specified.
     */
    public static final String AGENT_TYPE_JAVA = AgentTypes.AGENT_TYPE;

    public static final String AGENT_TYPE_NODEJS = "node";

    /**
     * Enterprise only, declared here so that the agent types are enumerated in one place.
     */
    public static final String AGENT_TYPE_DOTNET = "dotnet";

}
