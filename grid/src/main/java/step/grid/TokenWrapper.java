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
package step.grid;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import step.grid.tokenpool.Identity;
import step.grid.tokenpool.Interest;

public class TokenWrapper implements Identity {
	
	private Token token;
	
	private AgentRef agent;
	
	private final TokenHealth tokenHealth = new TokenHealth();
	
	private volatile TokenWrapperState state = TokenWrapperState.FREE;
	
	private TokenWrapperOwner currentOwner;
	
	private boolean hasSession = false;
	
	public TokenWrapper() {
		super();
	}

	public TokenWrapper(Token token, AgentRef agent) {
		super();
		this.token = token;	
		this.agent = agent;
	}

	@Override
	public Map<String, String> getAttributes() {
		return token.getAttributes();
	}

	@Override
	public Map<String, Interest> getInterests() {
		return token.getSelectionPatterns();
	}

	@JsonIgnore
	@Override
	public String getID() {
		return token.getId();
	}

	public Token getToken() {
		return token;
	}

	public AgentRef getAgent() {
		return agent;
	}

	public TokenWrapperOwner getCurrentOwner() {
		return currentOwner;
	}

	public void setCurrentOwner(TokenWrapperOwner currentOwner) {
		this.currentOwner = currentOwner;
	}

	public void setToken(Token token) {
		this.token = token;
	}

	public void setAgent(AgentRef agent) {
		this.agent = agent;
	}

	public boolean hasSession() {
		return hasSession;
	}

	public void setHasSession(boolean hasSession) {
		this.hasSession = hasSession;
	}

	public TokenHealth getTokenHealth() {
		return tokenHealth;
	}

	public TokenWrapperState getState() {
		return state;
	}

	protected void setState(TokenWrapperState state) {
		synchronized (this) {
			this.state = state;
		}
	}
	
	protected void performAtomically(Runnable runnable) {
		synchronized (this) {
			runnable.run();
		}
	}

	@Override
	public String toString() {
		return "AdapterToken [id=" + getID() + ", attributes=" + getAttributes() + ", interests=" + getInterests() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getAttributes() == null) ? 0 : getAttributes().hashCode());
		result = prime * result + ((getInterests() == null) ? 0 : getInterests().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TokenWrapper other = (TokenWrapper) obj;
		if (getAttributes() == null) {
			if (other.getAttributes() != null)
				return false;
		} else if (!getAttributes().equals(other.getAttributes()))
			return false;
		if (getInterests() == null) {
			if (other.getInterests() != null)
				return false;
		} else if (!getInterests().equals(other.getInterests()))
			return false;
		return true;
	}
}
