package step.functions.handler;

import step.grid.GridImpl;
import step.grid.ProxyGridServices;
import step.grid.TokenWrapper;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.client.GridClientConfiguration;
import step.grid.client.LocalGridClientImpl;
import step.grid.filemanager.FileManagerClient;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

import java.io.File;
import java.util.Map;

public class ProxyMessageHandler implements MessageHandler {

    @Override
    public OutputMessage handle(AgentTokenWrapper agentTokenWrapper, InputMessage inputMessage) throws Exception {

        FileManagerClient fileManagerClient = agentTokenWrapper.getServices().getFileManagerClient();
        ProxyGridServices.fileManagerClient = fileManagerClient;

        GridImpl grid = new GridImpl(new File("./filemanager"), 8090);
        grid.start();

        try {
            // Create a grid client to call keywords on this grid instance
            GridClientConfiguration gridClientConfiguration = new GridClientConfiguration();
            // Configure the selection timeout (this should be higher than the start time of the container
            gridClientConfiguration.setNoMatchExistsTimeout(60000);
            LocalGridClientImpl gridClient = new LocalGridClientImpl(gridClientConfiguration, grid);
            // Wait for the token to be available i.e. that the container started
            TokenWrapper tokenHandle = gridClient.getTokenHandle(Map.of(), Map.of(), true);
            // Execute a keyword using the selected token
            OutputMessage outputMessage = gridClient.call(tokenHandle.getID(), inputMessage.getPayload(), FunctionMessageHandler.class.getName(), inputMessage.getHandlerPackage(), inputMessage.getProperties(), 60000);
            return outputMessage;

        } finally {
            // Stop the grid
            grid.stop();
        }

    }
}
