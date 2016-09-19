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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentTokenPool {
	
	private static final Logger logger = LoggerFactory.getLogger(AgentTokenPool.class);
	
	private final Map<String, AgentTokenWrapper> pool = new HashMap<>();
	
	private long evictionTimeMs; 

	public AgentTokenPool(long evictionTimeMs) {
		super();
		this.evictionTimeMs = evictionTimeMs;
	}

	public synchronized List<AgentTokenWrapper> getTokens() {
		List<AgentTokenWrapper> result = new ArrayList<>(pool.size());
		result.addAll(pool.values());
		return result;
	}
	
	public synchronized void offerToken(AgentTokenWrapper token) {
		logger.debug("offerToken: " + token.toString());
		token.inUse = false;
		token.lastTouch = System.currentTimeMillis();
		token.session = new TokenSession();
		pool.put(token.getUid(), token);
	}
	
	public synchronized AgentTokenWrapper getToken(String tokenId) {
		AgentTokenWrapper token = pool.get(tokenId);
		if(token!=null) {
			if(token.inUse) {
				throw new RuntimeException("Token " + tokenId + " already in use. This should never happen!");
			} else {
				token.inUse = true;
			}
			logger.debug("getToken: " + token.toString());
		}
		return token;
		
	}
	
	public synchronized void returnToken(String tokenId) {
		AgentTokenWrapper token = pool.get(tokenId);
		token.inUse = false;
		token.lastTouch = System.currentTimeMillis();
		logger.debug("returnToken: " + token.toString());
	}
	
	public synchronized List<AgentTokenWrapper> evictSessions() {
		List<AgentTokenWrapper> evictedTokenss = new ArrayList<>();
		long currentTime = System.currentTimeMillis();
		for(AgentTokenWrapper token:pool.values()) {
			if(token.getSession()!=null && !token.inUse && token.lastTouch+evictionTimeMs<currentTime) {
				logger.debug("Evicting session. Token: " + token.toString());
				evictedTokenss.add(token);
				
				// TODO evict session
			}
		}
		return evictedTokenss;
	}
	
	
	

}
