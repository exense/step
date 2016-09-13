package step.grid.agent.conf;

import java.util.List;

public class AgentConf {
	
	String gridHost;
	
	Integer agentPort;
	
	String agentUrl;

	List<TokenGroupConf> tokenGroups;

	public AgentConf() {
		super();
	}

	public AgentConf(String gridHost, Integer agentPort, String agentUrl) {
		super();
		this.gridHost = gridHost;
		this.agentPort = agentPort;
		this.agentUrl = agentUrl;
	}

	public String getGridHost() {
		return gridHost;
	}

	public void setGridHost(String gridHost) {
		this.gridHost = gridHost;
	}

	public Integer getAgentPort() {
		return agentPort;
	}

	public void setAgentPort(Integer agentPort) {
		this.agentPort = agentPort;
	}

	public String getAgentUrl() {
		return agentUrl;
	}

	public void setAgentUrl(String agentUrl) {
		this.agentUrl = agentUrl;
	}

	public List<TokenGroupConf> getTokenGroups() {
		return tokenGroups;
	}

	public void setTokenGroups(List<TokenGroupConf> tokenGroups) {
		this.tokenGroups = tokenGroups;
	} 
}
