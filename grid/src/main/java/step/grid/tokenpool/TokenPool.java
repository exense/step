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
package step.grid.tokenpool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenPool<P extends Identity, F extends Identity> {
	
	private final static Logger logger = LoggerFactory.getLogger(TokenPool.class);
	
	final AffinityEvaluator affinityEval;
	
	final Map<String,Token<F>> tokens = new HashMap<>();
	
	final List<WaitingPretender<P,F>> waitingPretenders = Collections.synchronizedList(new LinkedList<WaitingPretender<P,F>>());
	
	long keepaliveTimeout;
	
	Timer keepaliveTimeoutCheckTimer;
	
	public TokenPool(AffinityEvaluator affinityEval) {
		super();
		this.affinityEval = affinityEval;
		
		keepaliveTimeout = -1;
		
		keepaliveTimeoutCheckTimer = new Timer();
		keepaliveTimeoutCheckTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					keepaliveTimeoutCheck();
				} catch (Exception e) {
					logger.error("An error occurred while running timer.", e);
				}
			}
		}, 10000,10000);
	}
	
	public void setKeepaliveTimeout(long timeout) {
		keepaliveTimeout = timeout;
	}
	
	public F selectToken(P pretender, long timeout) throws TimeoutException, InterruptedException {
		return selectToken(pretender, timeout, timeout);
	}
	
	public F selectToken(P pretender, long matchExistsTimeout, long noMatchExistsTimeout) throws TimeoutException, InterruptedException {
		boolean poolContainsMatchingToken = false;
		
		synchronized (tokens) {
			MatchingResult matchingResult =  searchMatchesInTokenList(pretender);
			Token<F> bestMatch = matchingResult.bestAvailableMatch;
			if(matchingResult.bestAvailableMatch!=null) {
				if(logger.isDebugEnabled())
					logger.debug("Found token without queuing. Pretender=" + pretender.toString() + ". Token="+bestMatch.toString() );
				bestMatch.available = false;
				return bestMatch.object;
			} else if(matchingResult.bestMatch!=null) {
				poolContainsMatchingToken = true;
			}
		}
		
		if(logger.isDebugEnabled())
			logger.debug("No free token found. Enqueuing... Pretender=" + pretender.toString());
		WaitingPretender<P, F> waitingPretender = new WaitingPretender<P,F>(pretender);
		try {
			waitingPretenders.add(waitingPretender);
			
			synchronized (waitingPretender) {
				long waitTime = poolContainsMatchingToken?matchExistsTimeout:noMatchExistsTimeout;
				waitingPretender.wait(waitTime);	
			}

			if(waitingPretender.associatedToken!=null) {
				if(logger.isDebugEnabled())
					logger.debug("Found token after queuing. Pretender=" + pretender.toString() + ". Token="+waitingPretender.associatedToken.toString());
				return waitingPretender.associatedToken.object;
			} else {
				logger.warn("Timeout occurred while selecting token. Pretender=" + pretender.toString());
				throw new TimeoutException("Timeout occurred while selecting token.");
			}
		} finally {
			waitingPretenders.remove(waitingPretender);
		}
		
	}
	
	private class MatchingResult {
		
		Token<F> bestMatch;
		
		Token<F> bestAvailableMatch;

		public MatchingResult(Token<F> bestMatch, Token<F> bestAvailableMatch) {
			super();
			this.bestMatch = bestMatch;
			this.bestAvailableMatch = bestAvailableMatch;
		}
		
	}
	
	private MatchingResult searchMatchesInTokenList(P pretender) {
		Token<F> bestMatch = null;
		int bestScore = -1;
		
		Token<F> bestAvailableMatch = null;
		int bestAvailableScore = -1;
		
		for(Token<F> token:tokens.values()) {
			int score = affinityEval.getAffinityScore(pretender, token.object);
			if(score!=-1 && score > bestScore) {
				bestScore = score;
				bestMatch = token;
			}
			if(token.available) {
				if(score!=-1 && score > bestAvailableScore) {
					bestAvailableScore = score;
					bestAvailableMatch = token;
				}
			}
		}
		return new MatchingResult(bestMatch, bestAvailableMatch);
	}
	
	private void notifyWaitingPretendersWithoutMatchInTokenList() {
		synchronized (waitingPretenders) {
			for(WaitingPretender<P, F> waitingPretender:waitingPretenders) {
				if(!hasWaitingPretenderAMatchInTokenList(waitingPretender)) {
					synchronized (waitingPretender) {
						waitingPretender.notify();						
					}
				}
			}
		}
	}
	
	private boolean hasWaitingPretenderAMatchInTokenList(WaitingPretender<P, F> waitingPretender) {
		MatchingResult matchingResult =  searchMatchesInTokenList(waitingPretender.pretender);
		return matchingResult.bestMatch!=null;
	}
	
	public void returnToken(F object) {
		synchronized (tokens) {
			if(logger.isDebugEnabled())
				logger.debug("Returning token. Token="+object.toString());
			Token<F> token = findToken(object);
			if(token.invalidated) {
				removeToken(token);
			} else {
				token.available = true;
				checkForMatchInPretenderWaitingQueue(token);
			}
		}
	}

	private void removeToken(Token<F> token) {
		tokens.remove(token.getObject().getID());
		notifyWaitingPretendersWithoutMatchInTokenList();
	}
	
	private Token<F> findToken(F object) {
		return tokens.get(object.getID());
	}
	
	
	public String offerToken(F object) {
		synchronized (tokens) {
			if(logger.isDebugEnabled())
				logger.debug("Offering token. Token="+object.toString());
			Token<F> token = findToken(object);
			if(token==null) {
				token = new Token<F>(object);
				token.available = true;
				tokens.put(token.object.getID(),token);
				checkForMatchInPretenderWaitingQueue(token);
			}
			keepaliveToken(token);
			
			return token.getObject().getID();
		}
	}
	
	private void keepaliveTimeoutCheck() {
		if(keepaliveTimeout>0) {
			synchronized (tokens) {
				long now = System.currentTimeMillis();
				List<Token<F>> invalidTokens = new ArrayList<>();
				for(Token<F> token:tokens.values()) {
					if(token.lastTouch+keepaliveTimeout<now) {
						invalidTokens.add(token);
					}
				}
				for(Token<F> token:invalidTokens) {
					invalidateToken(token);					
				}
			}
		}
	}
	
	
	public void keepaliveToken(String id) {
		synchronized (tokens) {
			Token<F> token = tokens.get(id);
			keepaliveToken(token);
		}
	}
	
	private void keepaliveToken(Token<F> token) {
		token.lastTouch = System.currentTimeMillis();
	}
	
	
	public void invalidate(String id) {
		synchronized (tokens) {
			Token<F> token = tokens.get(id);
			invalidateToken(token);
		}
	}
	
	public void invalidateToken(F object) {
		synchronized (tokens) {
			Token<F> token = findToken(object);
			invalidateToken(token);
		}
	}
	
	private void invalidateToken(Token<F> token) {
		if(token!=null) {
			if(logger.isDebugEnabled())
				logger.debug("Invalidating token. Token="+token.toString());
			token.invalidated = true;
			if(token.available) {
				removeToken(token);
			}
		}
	}
	
	
	private void checkForMatchInPretenderWaitingQueue(Token<F> token) {
		WaitingPretender<P, F> pretenderMatch = selectPretender(token);
		if(pretenderMatch!=null) {
			token.available = false;
			pretenderMatch.associatedToken = token;
			synchronized (pretenderMatch) {
				pretenderMatch.notify();				
			}
		}
	}
	
	private WaitingPretender<P, F> selectPretender(Token<F> token) {
		synchronized (waitingPretenders) {
			for(WaitingPretender<P, F> pretender:waitingPretenders) {
				if(pretender.associatedToken==null && affinityEval.getAffinityScore(pretender.pretender, token.object)>=0) {
					return pretender;
				}
			}			
		}
		return null;
	}
	
	public int getSize() {
		return tokens.size();
	}
	
	public List<Token<F>> getTokens() {
		synchronized (tokens) {
			return new ArrayList<>(tokens.values());
		}
	}
	
	public List<P> getWaitingPretenders() {
		synchronized (waitingPretenders) {
			List<P> result = new ArrayList<>(waitingPretenders.size());
			for(WaitingPretender<P, F> waitingPretender:waitingPretenders) {
				result.add(waitingPretender.pretender);
			}
			return result;
		}
	}
	
	public boolean existsAvailableMatchingToken(P pretender) {
		boolean poolContainsAvailableMatchingToken = false;
		
		synchronized (tokens) {
			MatchingResult matchingResult =  searchMatchesInTokenList(pretender);
			if(matchingResult.bestAvailableMatch != null) {
				poolContainsAvailableMatchingToken = true;
			}
			
			return poolContainsAvailableMatchingToken;
		}
		
	}

}
