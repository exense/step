package step.grid.agent.tokenpool;

import java.util.Map;
import java.util.UUID;

import step.grid.Token;
import step.grid.tokenpool.Interest;

public class AgentTokenWrapper {
	
	Token token;
	
	TokenSession session;

	Map<String, String> properties;
	
	boolean inUse;
	
	long lastTouch;
	

	public AgentTokenWrapper() {
		super();
		
		String uid = UUID.randomUUID().toString();
		
		token = new Token();
		token.setUid(uid);
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
		return token.getUid();
	}

	@Override
	public String toString() {
		return "Token [uid=" + getUid()
				+ ", attributes=" + getAttributes() + ", selectionPatterns="
				+ getSelectionPatterns() + "]";
	}

}
