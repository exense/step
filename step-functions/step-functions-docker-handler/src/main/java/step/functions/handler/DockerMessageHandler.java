package step.functions.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.agent.tokenpool.TokenReservationSession;
import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileVersionId;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class DockerMessageHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(DockerMessageHandler.class);

    // Use a static in order to have a singleton instance
    // Unfortunately the agent doesn't expose any agent-wide session to store singleton objects
    private static DockerContainerManager dockerContainerManager;

    // Properties passed from the controller via message properties
    public static final String MESSAGE_HANDLER = "$proxy.messageHandler";
    public static final String MESSAGE_HANDLER_FILE_ID = "$proxy.messageHandler.file.id";
    public static final String MESSAGE_HANDLER_FILE_VERSION = "$proxy.messageHandler.file.version";
    public static final String MESSAGE_HANDLER_INPUT = "$proxy.messageHandler.input";

    // AgentConf properties
    public static final String AGENT_CONF_DOCKER_SOCK = "docker.socket";
    public static final String AGENT_CONF_DOCKER_SOCK_DEFAULT = "unix:///var/run/docker.sock";
    public static final String AGENT_CONF_KUBERNETES_MODE = "docker.kubernetes";
    public static final String AGENT_CONF_DOCKER_AGENTLIB = "docker.agentlib";
    public static final String AGENT_CONF_DOCKER_CONTAINER_AGENT_STARTUP_TIMEOUT = "docker.container.agent.startup.timeoutms";

    @Override
    public OutputMessage handle(AgentTokenWrapper agentTokenWrapper, InputMessage inputMessage) throws Exception {
        Map<String, String> agentProperties = agentTokenWrapper.getProperties();
        Map<String, String> messageProperties = inputMessage.getProperties();

        synchronized (logger) {
            if (dockerContainerManager == null) {
                initDockerContainerManager(agentTokenWrapper, agentProperties);
            }
        }

        TokenReservationSession tokenReservationSession = agentTokenWrapper.getTokenReservationSession();
        DockerContainer dockerContainer = tokenReservationSession.get(DockerContainer.class);
        if (dockerContainer == null) {
            DockerMessageHandlerInput messageHandlerInput = DockerMessageHandlerInput.read(messageProperties.get(MESSAGE_HANDLER_INPUT));
            // Create a new docker container
            dockerContainer = dockerContainerManager.newContainer(messageHandlerInput.dockerRegistry, messageHandlerInput.dockerImage, messageHandlerInput.containerUser, messageHandlerInput.containerCmd);
            tokenReservationSession.put(dockerContainer);
        }

        // Get the initial message handler from the message properties
        String messageHandler = messageProperties.get(MESSAGE_HANDLER);
        String messageHandlerFileId = messageProperties.get(MESSAGE_HANDLER_FILE_ID);
        String messageHandlerFileVersion = messageProperties.get(MESSAGE_HANDLER_FILE_VERSION);
        FileVersionId messageHandlerFileVersionId = new FileVersionId(messageHandlerFileId, messageHandlerFileVersion);
        // Execute a keyword using the selected token
        return dockerContainer.call(inputMessage.getPayload(), messageHandler, messageHandlerFileVersionId, messageProperties, inputMessage.getCallTimeout());
    }

    private static void initDockerContainerManager(AgentTokenWrapper agentTokenWrapper, Map<String, String> agentProperties) throws IOException {
        FileManagerClient fileManagerClient = agentTokenWrapper.getServices().getFileManagerClient();

        DockerContainerManagerConfiguration dockerContainerManagerConfiguration = new DockerContainerManagerConfiguration();
        dockerContainerManagerConfiguration.dockerSocket = agentProperties.getOrDefault(AGENT_CONF_DOCKER_SOCK, AGENT_CONF_DOCKER_SOCK_DEFAULT);
        dockerContainerManagerConfiguration.kubernetesMode = Boolean.parseBoolean(agentProperties.getOrDefault(AGENT_CONF_KUBERNETES_MODE, Boolean.FALSE.toString()));
        if(agentProperties.containsKey(AGENT_CONF_DOCKER_CONTAINER_AGENT_STARTUP_TIMEOUT)) {
            dockerContainerManagerConfiguration.containerAgentStartupTimeoutMs = Long.parseLong(agentProperties.get(AGENT_CONF_DOCKER_CONTAINER_AGENT_STARTUP_TIMEOUT));
        }
        File agentLib;
        String agentLibProperty = agentProperties.get(AGENT_CONF_DOCKER_AGENTLIB);
        if(agentLibProperty != null) {
            agentLib = new File(agentLibProperty);
        } else {
            // Per default we assume that the agent has been started in the bin folder and that the lib are located in ../lib
            agentLib = new File("../lib");
        }

        dockerContainerManager = new DockerContainerManager(dockerContainerManagerConfiguration, fileManagerClient, agentLib);
    }
}
