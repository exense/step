package step.grid;

import java.util.List;

public class RegistrationMessage {
	
	private AgentRef agentRef;
	
	private List<Token> tokens;

	public RegistrationMessage() {
		super();
	}

	public RegistrationMessage(AgentRef agentRef, List<Token> tokens) {
		super();
		this.agentRef = agentRef;
		this.tokens = tokens;
	}

	public AgentRef getAgentRef() {
		return agentRef;
	}

	public void setAgentRef(AgentRef agentRef) {
		this.agentRef = agentRef;
	}

	public List<Token> getTokens() {
		return tokens;
	}

	public void setTokens(List<Token> tokens) {
		this.tokens = tokens;
	}

}
