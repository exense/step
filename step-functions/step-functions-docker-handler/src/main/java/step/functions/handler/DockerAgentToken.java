package step.functions.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.Grid;
import step.grid.TokenWrapper;
import step.grid.client.AbstractGridClientImpl;
import step.grid.client.GridClient;
import step.grid.client.GridClientException;

public class DockerAgentToken {

    private final Grid grid;
    private final GridClient gridClient;
    private final TokenWrapper tokenHandle;

    private static final Logger logger = LoggerFactory.getLogger(DockerAgentToken.class);


    public DockerAgentToken(Grid grid, GridClient gridClient, TokenWrapper tokenHandle) {
        this.grid = grid;
        this.gridClient = gridClient;
        this.tokenHandle = tokenHandle;
    }

    public TokenWrapper getTokenHandle() {
        return tokenHandle;
    }

    public void returnToken() throws GridClientException, AbstractGridClientImpl.AgentCommunicationException {
        logger.info(String.format("Returning tokenHandle %s", tokenHandle.getID()));
        gridClient.returnTokenHandle(tokenHandle.getID());
    }

    public void invalidateToken() {
        logger.info(String.format("Removing token %s", tokenHandle.getID()));
        grid.invalidateToken(tokenHandle.getID());
    }

}
