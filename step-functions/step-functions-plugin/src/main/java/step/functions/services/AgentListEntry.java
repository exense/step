package step.functions.services;

import java.util.List;

import step.grid.AgentRef;
import step.grid.TokenWrapper;
import step.grid.reports.TokenGroupCapacity;

public class AgentListEntry {

	private AgentRef agentRef;
	
	private List<TokenWrapper> tokens;
	
	private TokenGroupCapacity tokensCapacity;

	public AgentListEntry() {
		super();
	}

	public AgentListEntry(AgentRef agentRef, List<TokenWrapper> tokens) {
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

	public List<TokenWrapper> getTokens() {
		return tokens;
	}

	public void setTokens(List<TokenWrapper> tokens) {
		this.tokens = tokens;
	}

	public TokenGroupCapacity getTokensCapacity() {
		return tokensCapacity;
	}

	public void setTokensCapacity(TokenGroupCapacity tokensCapacity) {
		this.tokensCapacity = tokensCapacity;
	}
}
