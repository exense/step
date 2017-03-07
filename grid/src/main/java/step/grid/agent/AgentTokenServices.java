package step.grid.agent;

import java.util.Map;

import step.common.isolation.ContextManager;
import step.grid.filemanager.FileManagerClient;

public class AgentTokenServices {
	
	FileManagerClient fileManagerClient;
	
	ContextManager contextManager;
	
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

	public ContextManager getContextManager() {
		return contextManager;
	}

	public void setContextManager(ContextManager contextManager) {
		this.contextManager = contextManager;
	}
}
