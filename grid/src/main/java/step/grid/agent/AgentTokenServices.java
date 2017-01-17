package step.grid.agent;

import step.grid.filemanager.FileManagerClient;

public class AgentTokenServices {
	
	FileManagerClient fileManagerClient;
	
	public AgentTokenServices(FileManagerClient fileManagerClient) {
		super();
		
		this.fileManagerClient = fileManagerClient;
	}

	public FileManagerClient getFileManagerClient() {
		return fileManagerClient;
	}
}
