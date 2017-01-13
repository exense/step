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
package step.grid.agent.tokenpool;

import java.util.Map;
import java.util.UUID;

import step.grid.Token;
import step.grid.agent.AgentTokenServices;
import step.grid.tokenpool.Interest;

public class AgentTokenWrapper {
	
	Token token;
	
	TokenSession session;
	
	AgentTokenServices services;
	
	Map<String, String> properties;
	
	boolean inUse;
	
	long lastTouch;
	

	public AgentTokenWrapper() {
		super();
		
		String uid = UUID.randomUUID().toString();
		
		token = new Token();
		token.setId(uid);
	}

	public TokenSession getSession() {
		return session;
	}

	public void setSession(TokenSession session) {
		this.session = session;
	}

	public Token getToken() {
		return token;
	}

	public Map<String, String> getAttributes() {
		return token.getAttributes();
	}

	public void setAttributes(Map<String, String> attributes) {
		token.setAttributes(attributes);
	}

	public Map<String, Interest> getSelectionPatterns() {
		return token.getSelectionPatterns();
	}

	public void setSelectionPatterns(Map<String, Interest> selectionPatterns) {
		token.setSelectionPatterns(selectionPatterns);
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public long getLastTouch() {
		return lastTouch;
	}

	public void setLastTouch(long lastTouch) {
		this.lastTouch = lastTouch;
	}

	public String getUid() {
		return token.getId();
	}

	public boolean isInUse() {
		return inUse;
	}

	public AgentTokenServices getServices() {
		return services;
	}

	public void setServices(AgentTokenServices services) {
		this.services = services;
	}

	@Override
	public String toString() {
		return "Token [uid=" + getUid()
				+ ", attributes=" + getAttributes() + ", selectionPatterns="
				+ getSelectionPatterns() + "]";
	}

}
