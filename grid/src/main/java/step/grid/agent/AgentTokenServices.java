package step.grid.agent;

import java.util.Map;

import step.grid.filemanager.FileManagerClient;

public class AgentTokenServices {
	
	FileManagerClient fileManagerClient;
	
	Map<String,String> agentProperties;
	
	public AgentTokenServices(FileManagerClient fileManagerClient) {
		super();
		
		this.fileManagerClient = fileManagerClient;
	}

	public FileManagerClient getFileManagerClient() {
		return fileManagerClient;
	}

	public Map<String, String> getAgentProperties() {
		return agentProperties;
	}

	protected void setAgentProperties(Map<String, String> agentProperties) {
		this.agentProperties = agentProperties;
	}
}
