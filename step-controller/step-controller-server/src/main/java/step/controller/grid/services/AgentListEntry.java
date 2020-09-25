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
package step.controller.grid.services;

import java.util.List;

import step.grid.AgentRef;
import step.grid.TokenWrapper;
import step.grid.reports.TokenGroupCapacity;

public class AgentListEntry {

	private AgentRef agentRef;
	
	private List<TokenWrapper> tokens;
	
	private TokenGroupCapacity tokensCapacity;

	public AgentListEntry() {
		super();
	}

	public AgentListEntry(AgentRef agentRef, List<TokenWrapper> tokens) {
		super();
		this.agentRef = agentRef;
		this.tokens = tokens;
	}

	public AgentRef getAgentRef() {
		return agentRef;
	}

	public void setAgentRef(AgentRef agentRef) {
		this.agentRef = agentRef;
	}

	public List<TokenWrapper> getTokens() {
		return tokens;
	}

	public void setTokens(List<TokenWrapper> tokens) {
		this.tokens = tokens;
	}

	public TokenGroupCapacity getTokensCapacity() {
		return tokensCapacity;
	}

	public void setTokensCapacity(TokenGroupCapacity tokensCapacity) {
		this.tokensCapacity = tokensCapacity;
	}
}
