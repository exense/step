package step.grid;

import java.util.Map;

import step.grid.tokenpool.Interest;



public class Token {
	
	String id;
	
	String agentid;
	
	Map<String, String> attributes;
	
	Map<String, Interest> selectionPatterns;

	public Token() {
		super();
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public String getAgentid() {
		return agentid;
	}

	public void setAgentid(String agentid) {
		this.agentid = agentid;
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
