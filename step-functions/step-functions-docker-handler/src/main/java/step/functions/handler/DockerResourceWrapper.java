package step.functions.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.client.AbstractGridClientImpl;
import step.grid.client.GridClientException;

import java.io.Closeable;
import java.io.IOException;

public class DockerResourceWrapper implements Closeable {

	private DockerContainer dockerContainer;
	private DockerAgentToken dockerAgentToken;

	private static final Logger logger = LoggerFactory.getLogger(DockerResourceWrapper.class);

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
	public void close() throws IOException {
		// return token from grid client and release session on docker agent
		try {
			dockerAgentToken.returnToken();
		} catch (GridClientException | AbstractGridClientImpl.AgentCommunicationException e) {
			logger.error(e.toString());
		}
		//Stop the docker container
		dockerContainer.close();
		//Remove token from local grid
		dockerAgentToken.invalidateToken();
	}
}
