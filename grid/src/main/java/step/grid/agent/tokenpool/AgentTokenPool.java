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
	
	public AgentTokenPool() {
		super();
	}

	public synchronized List<AgentTokenWrapper> getTokens() {
		List<AgentTokenWrapper> result = new ArrayList<>(pool.size());
		result.addAll(pool.values());
		return result;
	}
	
	public synchronized void offerToken(AgentTokenWrapper token) {
		logger.debug("offerToken: " + token.toString());
		pool.put(token.getUid(), token);
	}
	
	public synchronized AgentTokenWrapper getTokenForExecution(String tokenId) throws InvalidTokenIdException {
		AgentTokenWrapper token = pool.get(tokenId);
		if(token!=null) {
			if(token.tokenReservationSession==null) {
				token.tokenReservationSession = new TokenReservationSession() {
					@Override
					public Object get(String arg0) {
						throw unusableSessionException();
					}

					@Override
					public Object put(String arg0, Object arg1) {
						throw unusableSessionException();
					}

					private RuntimeException unusableSessionException() {
						// TODO use error codes instead of error messages
						return new RuntimeException("Session object unreachable. Wrap your keywords with a Session node in your test plan in order to make the session object available.");
					}
				};
			}
		} else {
			throw new InvalidTokenIdException();
		}
		return token;
	}
	
	private AgentTokenWrapper getToken(String tokenId) {
		AgentTokenWrapper token = pool.get(tokenId);
		return token;
	}
	
	public synchronized void createTokenReservationSession(String tokenId) throws InvalidTokenIdException {
		AgentTokenWrapper token = getToken(tokenId);
		if(token!=null) {
			TokenReservationSession previousTokenReservationSession = token.getTokenReservationSession();
			if(previousTokenReservationSession!=null) {
				logger.warn("Trying to reserve token '"+tokenId+"' which is already reserved. Closing previous session.");
				try {
					previousTokenReservationSession.close();
				} catch (Exception e) {
					logger.warn("Error while closing token session for token "+tokenId+
							". This may cause a resource leak. Creating new session anyway.", e);
				}
			}
			
			TokenReservationSession tokenReservationContext = new TokenReservationSession();
			token.setTokenReservationSession(tokenReservationContext);
		} else {
			throw new InvalidTokenIdException();
		}
	}
	
	public synchronized void closeTokenReservationSession(String tokenId) throws InvalidTokenIdException {
		AgentTokenWrapper token = getToken(tokenId);
		if(token!=null) {
			TokenReservationSession tokenReservationSession = token.getTokenReservationSession();
			token.setTokenReservationSession(null);
			if(tokenReservationSession!=null) {
				try {
					tokenReservationSession.close();					
				} catch (Exception e) {
					logger.warn("Error while closing token session for token "+tokenId+
							". This may cause a resource leak. Still returning token to the pool.", e);
				}
			} else {
				// token has already been released or has never been reserved. Nothing to do.
				logger.warn("Trying to release token '"+tokenId+"' which is not reserved");
			}
		} else {
			throw new InvalidTokenIdException();
		}
	}
	
	@SuppressWarnings("serial")
	public static class InvalidTokenIdException extends Exception {
		
	}
}
