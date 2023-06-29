package step.functions.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.GridImpl;
import step.grid.ProxyGridServices;
import step.grid.TokenWrapper;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.agent.tokenpool.TokenReservationSession;
import step.grid.client.GridClientConfiguration;
import step.grid.client.LocalGridClientImpl;
import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileVersionId;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyMessageHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyMessageHandler.class);

    // Use a static in order to have a singleton instance of the Grid.
    // Unfortunately the agent doesn't expose any agent-wide session to store singleton objects
    private static final ConcurrentHashMap<String, GridImpl> gridMap = new ConcurrentHashMap<>();

    // Constants
    public static final String GRID = "grid";
    public static final String CONTAINER_NAME = "agent";

    // Properties passed from the controller via message properties
    public static final String MESSAGE_HANDLER = "$proxy.messageHandler";
    public static final String MESSAGE_HANDLER_FILE_ID = "$proxy.messageHandler.file.id";
    public static final String MESSAGE_HANDLER_FILE_VERSION = "$proxy.messageHandler.file.version";

    // AgentConf properties
    public static final String AGENT_CONF_DOCKER_LOCALGRID_PORT = "docker.localgrid.port";

    @Override
    public OutputMessage handle(AgentTokenWrapper agentTokenWrapper, InputMessage inputMessage) throws Exception {
        Map<String, String> agentProperties = agentTokenWrapper.getProperties();
        Map<String, String> messageProperties = inputMessage.getProperties();
        String localGridPortStr = agentProperties.getOrDefault(AGENT_CONF_DOCKER_LOCALGRID_PORT, "8090");
        int localGridPort = Integer.parseInt(localGridPortStr);

        TokenReservationSession tokenReservationSession = agentTokenWrapper.getTokenReservationSession();
        DockerContainer container = tokenReservationSession.get(DockerContainer.class);
        if (container == null) {
            // Create a new docker container
            container = new DockerContainer(agentProperties, messageProperties, localGridPort);
            // Add the container to the session. It will be closed automatically at session release
            tokenReservationSession.put(container);
        }

        FileManagerClient fileManagerClient = agentTokenWrapper.getServices().getFileManagerClient();
        ProxyGridServices.fileManagerClient = fileManagerClient;

        // Use a concurrent hash map to create a grid singleton
        GridImpl grid = gridMap.computeIfAbsent(GRID, k -> createGrid(localGridPort));
        // Create a grid client to call keywords on this grid instance
        GridClientConfiguration gridClientConfiguration = new GridClientConfiguration();
        // Configure the selection timeout (this should be higher than the start time of the container
        gridClientConfiguration.setNoMatchExistsTimeout(60000);
        LocalGridClientImpl gridClient = new LocalGridClientImpl(gridClientConfiguration, grid);
        // Wait for the token to be available i.e. that the container started
        TokenWrapper tokenHandle = gridClient.getTokenHandle(Map.of(), Map.of(), true);
        // Get the initial message handler from the message properties
        String messageHandler = messageProperties.get(MESSAGE_HANDLER);
        String messageHandlerFileId = messageProperties.get(MESSAGE_HANDLER_FILE_ID);
        String messageHandlerFileVersion = messageProperties.get(MESSAGE_HANDLER_FILE_VERSION);
        FileVersionId messageHandlerFileVersionId = new FileVersionId(messageHandlerFileId, messageHandlerFileVersion);
        // Execute a keyword using the selected token
        return gridClient.call(tokenHandle.getID(), inputMessage.getPayload(), messageHandler, messageHandlerFileVersionId, messageProperties, 60000);
    }

    private static GridImpl createGrid(int port) {
        GridImpl grid = new GridImpl(new File("./filemanager"), port);
        try {
            grid.start();
        } catch (Exception e) {
            throw new RuntimeException("Error while starting sub-agent grid on port " + port, e);
        }
        return grid;
    }


}
