package step.grid.agent.conf;

import java.util.List;

public class AgentConf {

	List<TokenGroupConf> tokenGroups;

	public AgentConf() {
		super();
	}

	public List<TokenGroupConf> getTokenGroups() {
		return tokenGroups;
	}

	public void setTokenGroups(List<TokenGroupConf> tokenGroups) {
		this.tokenGroups = tokenGroups;
	} 
}
