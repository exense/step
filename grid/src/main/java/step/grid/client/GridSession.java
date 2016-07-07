package step.grid.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import step.grid.TokenWrapper;
import step.grid.tokenpool.Identity;
import step.grid.tokenpool.Interest;

public class GridSession {

	private final String sessionID;
	
	private List<Entry> tokens = new ArrayList<Entry>();
	
	public GridSession(String sessionID) {
		super();
		this.sessionID = sessionID;
	}
	
	public String getSessionID() {
		return sessionID;
	}

	public TokenWrapper getToken(Identity type) {
		synchronized (tokens) {
			Map<String, Interest> mustCriteria = getMustCriteria(type);
			for(Entry entry:tokens) {
				if(type.getAttributes()!=null && mustCriteria !=null) {
					if(type.getAttributes().equals(entry.identity.getAttributes()) &&
							mustCriteria.equals(getMustCriteria(entry.identity))) {
						return entry.token;
					}
				} else {
					throw new RuntimeException("Attributes or selectionCriteria are null. This shouldn't happen.");
				}
			}
			return null;		
		}
	}
	
	private Map<String, Interest> getMustCriteria(Identity type) {
		if(type.getInterests()!=null) {
			Map<String, Interest> result = new HashMap<>();
			for(Map.Entry<String, Interest> entry:type.getInterests().entrySet()) {
				if(entry.getValue().isMust()) {
					result.put(entry.getKey(), entry.getValue());
				}
			}
			return result;
		} else {
			return null;
		}
	}
	
	public void putToken(Identity type, TokenWrapper token) {
		synchronized (tokens) {
			tokens.add(new Entry(type, token));
		}
	}
	
	public Collection<TokenWrapper> getAllTokens() {
		List<TokenWrapper> allTokens = new ArrayList<>();
		synchronized (tokens) {
			for(Entry entry:tokens) {
				allTokens.add(entry.token);
			}
		}
		return allTokens;
	}
	
	private class Entry {
		Identity identity;
		
		TokenWrapper token;

		public Entry(Identity identity, TokenWrapper token) {
			super();
			this.identity = identity;
			this.token = token;
		}
	}
}
