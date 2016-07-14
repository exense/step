package step.grid;

public class AgentRef {
	
	private String agentId;
	
	private String agentUrl;

	public AgentRef() {
		super();
	}

	public AgentRef(String agentId, String agentUrl) {
		super();
		this.agentId = agentId;
		this.agentUrl = agentUrl;
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getAgentUrl() {
		return agentUrl;
	}

	public void setAgentUrl(String agentUrl) {
		this.agentUrl = agentUrl;
	}

	@Override
	public String toString() {
		return "AgentRef [agentId=" + agentId + ", agentUrl=" + agentUrl + "]";
	}

}
