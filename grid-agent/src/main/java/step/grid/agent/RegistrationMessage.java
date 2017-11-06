/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.grid.agent;

import java.util.List;

import step.grid.AgentRef;
import step.grid.Token;

public class RegistrationMessage {
	
	private AgentRef agentRef;
	
	private List<Token> tokens;

	public RegistrationMessage() {
		super();
	}

	public RegistrationMessage(AgentRef agentRef, List<Token> tokens) {
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

	public List<Token> getTokens() {
		return tokens;
	}

	public void setTokens(List<Token> tokens) {
		this.tokens = tokens;
	}

}
