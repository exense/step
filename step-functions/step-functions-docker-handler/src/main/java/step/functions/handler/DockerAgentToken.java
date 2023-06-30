package step.functions.handler;

import step.grid.TokenWrapper;
import step.grid.client.AbstractGridClientImpl;
import step.grid.client.GridClient;
import step.grid.client.GridClientException;

import java.io.Closeable;
import java.io.IOException;

public class DockerAgentToken implements Closeable {

    private final GridClient gridClient;
    private final TokenWrapper tokenHandle;

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
            gridClient.returnTokenHandle(tokenHandle.getID());
        } catch (GridClientException e) {
            throw new RuntimeException(e);
        } catch (AbstractGridClientImpl.AgentCommunicationException e) {
            throw new RuntimeException(e);
        }
    }
}
