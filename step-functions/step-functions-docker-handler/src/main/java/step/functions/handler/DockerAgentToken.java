package step.functions.handler;

import step.grid.TokenWrapper;
import step.grid.client.AbstractGridClientImpl;
import step.grid.client.GridClientException;
import step.grid.client.LocalGridClientImpl;

import java.io.Closeable;
import java.io.IOException;

public class DockerAgentToken implements Closeable {

    private final LocalGridClientImpl gridClient;
    private final TokenWrapper tokenHandle;

    public DockerAgentToken(LocalGridClientImpl gridClient, TokenWrapper tokenHandle) {
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
