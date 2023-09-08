package step.functions.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.TokenWrapper;
import step.grid.client.AbstractGridClientImpl;
import step.grid.client.GridClient;
import step.grid.client.GridClientException;

import java.io.Closeable;
import java.io.IOException;

public class DockerAgentToken implements Closeable {

    private final GridClient gridClient;
    private final TokenWrapper tokenHandle;

    private static final Logger logger = LoggerFactory.getLogger(DockerAgentToken.class);


    public DockerAgentToken(GridClient gridClient, TokenWrapper tokenHandle) {
        this.gridClient = gridClient;
        this.tokenHandle = tokenHandle;
    }

    public TokenWrapper getTokenHandle() {
        return tokenHandle;
    }

    @Override
    public void close() throws IOException {
        try {
            logger.info(String.format("Marking tokenHandle %s as Failing", tokenHandle.getID()));
            gridClient.markTokenAsFailing(tokenHandle.getID(), "Failing", new Exception("Failing"));
            logger.info(String.format("Returning tokenHandle %s", tokenHandle.getID()));
            gridClient.returnTokenHandle(tokenHandle.getID());
            logger.info(String.format("TokenHandle %s returned", tokenHandle.getID()));
        } catch (GridClientException | AbstractGridClientImpl.AgentCommunicationException e) {
            throw new RuntimeException(e);
        }
    }
}
