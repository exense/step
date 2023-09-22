package step.functions.handler;

public class DockerResourceWrapper implements AutoCloseable {

	private DockerContainer dockerContainer;
	private DockerAgentToken dockerAgentToken;

	public DockerContainer getDockerContainer() {
		return dockerContainer;
	}

	public void setDockerContainer(DockerContainer dockerContainer) {
		this.dockerContainer = dockerContainer;
	}

	public DockerAgentToken getDockerAgentToken() {
		return dockerAgentToken;
	}

	public void setDockerAgentToken(DockerAgentToken dockerAgentToken) {
		this.dockerAgentToken = dockerAgentToken;
	}

	@Override
	public void close() throws Exception {
		// return token from grid client and release session on docker agent
		dockerAgentToken.returnToken();
		//Stop the docker container
		dockerContainer.close();
		//Remove token from local grid
		dockerAgentToken.invalidateToken();
	}
}
