package step.grid.agent;

import java.util.Map;

import step.grid.filemanager.FileManagerClient;
import step.grid.isolation.ApplicationContextBuilder;
import step.grid.isolation.ContextManager;

public class AgentTokenServices {
	
	FileManagerClient fileManagerClient;
	
	ContextManager contextManager;
	
	Map<String,String> agentProperties;
	
	ApplicationContextBuilder applicationContextBuilder;
	
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

	public void setAgentProperties(Map<String, String> agentProperties) {
		this.agentProperties = agentProperties;
	}

	public ContextManager getContextManager() {
		return contextManager;
	}

	public void setContextManager(ContextManager contextManager) {
		this.contextManager = contextManager;
	}

	public ApplicationContextBuilder getApplicationContextBuilder() {
		return applicationContextBuilder;
	}

	public void setApplicationContextBuilder(ApplicationContextBuilder applicationContextBuilder) {
		this.applicationContextBuilder = applicationContextBuilder;
	}
}
