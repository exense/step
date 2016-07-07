package step.grid;

import java.util.Map;

import step.grid.tokenpool.Interest;



public class Token {
	
	String agentid;
	
	String uid;
	
	Map<String, String> attributes;
	
	Map<String, Interest> selectionPatterns;

	public Token() {
		super();
	}

	public String getAgentid() {
		return agentid;
	}

	public void setAgentid(String agentid) {
		this.agentid = agentid;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public Map<String, Interest> getSelectionPatterns() {
		return selectionPatterns;
	}

	public void setSelectionPatterns(Map<String, Interest> selectionPatterns) {
		this.selectionPatterns = selectionPatterns;
	}
}
