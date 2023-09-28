package step.functions.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.GridImpl;
import step.grid.ProxyGridServices;
import step.grid.TokenWrapper;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.agent.tokenpool.TokenReservationSession;
import step.grid.client.GridClient;
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
    private static final ConcurrentHashMap<String, Object> gridMap = new ConcurrentHashMap<>();

    // Constants
    public static final String GRID = "grid";

    // Properties passed from the controller via message properties
    public static final String MESSAGE_HANDLER = "$proxy.messageHandler";
    public static final String MESSAGE_HANDLER_FILE_ID = "$proxy.messageHandler.file.id";
    public static final String MESSAGE_HANDLER_FILE_VERSION = "$proxy.messageHandler.file.version";

    // AgentConf properties
    public static final String AGENT_CONF_DOCKER_LOCALGRID_PORT = "docker.localgrid.port";

    // Maximum time for the container to start
    private static final long CONTAINER_STARTUP_OFFSET = 120_000;

    @Override
    public OutputMessage handle(AgentTokenWrapper agentTokenWrapper, InputMessage inputMessage) throws Exception {
        Map<String, String> agentProperties = agentTokenWrapper.getProperties();
        Map<String, String> messageProperties = inputMessage.getProperties();
        long keywordTimeoutMs = inputMessage.getCallTimeout();
        // TODO set timeout according to keyword execution timeout
        final long noMatchExistsTimeout = (keywordTimeoutMs - CONTAINER_STARTUP_OFFSET) <= 0 ? keywordTimeoutMs - 30_000 : keywordTimeoutMs - CONTAINER_STARTUP_OFFSET;

        messageProperties.forEach((k,v)-> System.out.println(k + " : " + v));
        String localGridPortStr = agentProperties.getOrDefault(AGENT_CONF_DOCKER_LOCALGRID_PORT, "8090");
        int localGridPort = Integer.parseInt(localGridPortStr);

        TokenReservationSession tokenReservationSession = agentTokenWrapper.getTokenReservationSession();
        DockerResourceWrapper dockerResourceWrapper = getDockerResourceWrapper(tokenReservationSession);
        DockerContainer container = dockerResourceWrapper.getDockerContainer();
        if (container == null) {
            logger.info("Creating new container");
            // Create a new docker container
            container = new DockerContainer(agentProperties, messageProperties, localGridPort);
            // Add the container to the session. It will be closed automatically at session release
            dockerResourceWrapper.setDockerContainer(container);
            logger.debug("Container created");
        }

        FileManagerClient fileManagerClient = agentTokenWrapper.getServices().getFileManagerClient();
        ProxyGridServices.fileManagerClient = fileManagerClient;

        // Use a concurrent hash map to create singleton
        GridImpl grid = (GridImpl) gridMap.computeIfAbsent(GRID, k -> createGrid(localGridPort));
        GridClient gridClient = (GridClient) gridMap.computeIfAbsent("gridClient", k -> {
            // Create a grid client to call keywords on this grid instance
            GridClientConfiguration gridClientConfiguration = new GridClientConfiguration();
            // Configure the selection timeout (this should be higher than the start time of the container)
            gridClientConfiguration.setNoMatchExistsTimeout(noMatchExistsTimeout);
            return new LocalGridClientImpl(gridClientConfiguration, grid);
        });

        // Get the previously reserved token from the session if any
        DockerAgentToken token = dockerResourceWrapper.getDockerAgentToken();
        if(token == null) {
            // Wait for the token to be available i.e. that the container started
            TokenWrapper tokenHandle = gridClient.getTokenHandle(Map.of(), Map.of(), true);
            // Add the token to the session. It will be returned to the pool after session release
            token = new DockerAgentToken(grid, gridClient, tokenHandle);
            dockerResourceWrapper.setDockerAgentToken(token);
        }

        // Get the initial message handler from the message properties
        String messageHandler = messageProperties.get(MESSAGE_HANDLER);
        String messageHandlerFileId = messageProperties.get(MESSAGE_HANDLER_FILE_ID);
        String messageHandlerFileVersion = messageProperties.get(MESSAGE_HANDLER_FILE_VERSION);
        FileVersionId messageHandlerFileVersionId = new FileVersionId(messageHandlerFileId, messageHandlerFileVersion);
        // Execute a keyword using the selected token
        return gridClient.call(token.getTokenHandle().getID(), inputMessage.getPayload(), messageHandler, messageHandlerFileVersionId, messageProperties, (int) noMatchExistsTimeout);
    }

    private DockerResourceWrapper getDockerResourceWrapper(TokenReservationSession tokenReservationSession) {
        DockerResourceWrapper dockerResourceWrapper = tokenReservationSession.get(DockerResourceWrapper.class);
        if (dockerResourceWrapper == null) {
            dockerResourceWrapper = new DockerResourceWrapper();
            tokenReservationSession.put(dockerResourceWrapper);
        }
        return dockerResourceWrapper;
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
