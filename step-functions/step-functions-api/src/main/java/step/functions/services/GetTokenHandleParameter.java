package step.functions.services;

import java.util.Map;

import step.grid.tokenpool.Interest;

public class GetTokenHandleParameter {
	
	Map<String, String> attributes;
	Map<String, Interest> interests;
	boolean createSession;
	boolean local;

	public Map<String, String> getAttributes() {
		return attributes;
	}
	
	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
	
	public Map<String, Interest> getInterests() {
		return interests;
	}
	
	public void setInterests(Map<String, Interest> interests) {
		this.interests = interests;
	}
	
	public boolean isCreateSession() {
		return createSession;
	}
	
	public void setCreateSession(boolean createSession) {
		this.createSession = createSession;
	}

	public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
	}
}