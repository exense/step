package step.functions.handler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.GridImpl;
import step.grid.ProxyGridServices;
import step.grid.TokenWrapper;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.client.GridClientConfiguration;
import step.grid.client.LocalGridClientImpl;
import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileVersionId;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
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
    public static final String MESSAGE_PROP_DOCKER_REGISTRY_URL = "$docker.registryUrl";
    public static final String MESSAGE_PROP_DOCKER_REGISTRY_USERNAME = "$docker.registryUsername";
    public static final String MESSAGE_PROP_DOCKER_REGISTRY_PASSWORD = "$docker.registryPassword";
    public static final String MESSAGE_PROP_DOCKER_IMAGE = "$docker.image";

    // AgentConf properties
    public static final String AGENT_CONF_DOCKER_SOCK = "docker.sock";
    public static final String AGENT_CONF_DOCKER_SOCK_DEFAULT = "unix:///var/run/docker.sock";
    public static final String AGENT_CONF_DOCKER_IN_DOCKER = "$docker.in.docker";
    public static final String AGENT_CONF_DOCKER_LOCALGRID_PORT = "docker.localgrid.port";

    @Override
    public OutputMessage handle(AgentTokenWrapper agentTokenWrapper, InputMessage inputMessage) throws Exception {
        Map<String, String> agentProperties = agentTokenWrapper.getProperties();
        String dockerHost = agentProperties.getOrDefault(AGENT_CONF_DOCKER_SOCK, AGENT_CONF_DOCKER_SOCK_DEFAULT);

        Map<String, String> messageProperties = inputMessage.getProperties();
        String registryUrl = messageProperties.get(MESSAGE_PROP_DOCKER_REGISTRY_URL);
        String registryUsername = messageProperties.get(MESSAGE_PROP_DOCKER_REGISTRY_USERNAME);
        String registryPassword = messageProperties.get(MESSAGE_PROP_DOCKER_REGISTRY_PASSWORD);
        String dockerImage = messageProperties.get(MESSAGE_PROP_DOCKER_IMAGE);
        String localGridPortStr = agentProperties.getOrDefault(AGENT_CONF_DOCKER_LOCALGRID_PORT, "8090");
        int localGridPort = Integer.parseInt(localGridPortStr);
        boolean dockerInDocker = agentProperties.containsKey(AGENT_CONF_DOCKER_IN_DOCKER);

        logger.info("Docker in docker configuration detected : " + dockerInDocker);

        DockerClient dockerClient = initDockerClient(registryUrl, registryUsername, registryPassword, dockerHost);
        CreateContainerResponse container = startContainer(dockerClient, dockerImage);
        copyAgentMaterialAndStart(dockerClient, container, dockerInDocker, localGridPort);

        FileManagerClient fileManagerClient = agentTokenWrapper.getServices().getFileManagerClient();
        ProxyGridServices.fileManagerClient = fileManagerClient;

        // Use the concurrent hash map to create a grid singleton
        GridImpl grid = gridMap.computeIfAbsent(GRID, k -> createGrid(localGridPort));
        try {
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
        } finally {
            stopContainer(dockerClient);
        }
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

    private DockerClient initDockerClient(String registryUrl, String registryUsername, String registryPassword, String dockerHost) {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withRegistryUrl(registryUrl)
                .withRegistryUsername(registryUsername)
                .withRegistryPassword(registryPassword)
                .withDockerHost(dockerHost)
                .build();
        ApacheDockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        return DockerClientImpl.getInstance(config, dockerHttpClient);
    }

    private CreateContainerResponse startContainer(DockerClient dockerClient, String image) throws InterruptedException {
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
        pullImageCmd.exec(new PullImageResultCallback()).awaitCompletion();
        CreateContainerResponse container = dockerClient.createContainerCmd(image)
                .withName(CONTAINER_NAME)
                //.withExposedPorts(new ExposedPort(input.getInt("exposedPort")))
                //.withPortBindings(PortBinding.parse(input.getString("portBindings")))
                .withHostConfig(new HostConfig().withNetworkMode("host")/*.withPortBindings(new PortBinding(Ports.Binding.bindPort(30000), ExposedPort.tcp(30000)))*/)
                // Command used only for testing
                .withCmd("bash", "-c", "sleep 300")
                .exec();
        dockerClient.startContainerCmd(container.getId()).exec();
        InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(container.getId()).exec();
        logger.info("Container startup response : " + inspectContainerResponse.toString());
        return container;
    }

    private void copyAgentMaterialAndStart(DockerClient dockerClient, CreateContainerResponse container, boolean dockerInDocker, int gridPort) throws InterruptedException, IOException {
        StringBuilder stringBuilder = new StringBuilder();
        final StringBuilderLogReader callback = new StringBuilderLogReader(stringBuilder);

        copyLocalFolderToContainer(dockerClient, container, "conf");
        copyLocalFolderToContainer(dockerClient, container, "bin");
        copyLocalFolderToContainer(dockerClient, container, "lib");


        // Files are copied as root, we need to change the ownership
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(container.getId())
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withUser("root")
                .withCmd("bash", "-c", "chown -R agent:agent /home/agent")
                .exec();
        dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .exec(new StringBuilderLogReader(stringBuilder))
                .awaitCompletion();

        String startupCmd;
        String gridHost;
        if (dockerInDocker) {
            gridHost = System.getenv("POD_IP");
        } else {
            gridHost = "localhost";
        }

        String subGridUrl = "http://" + gridHost + ":" + gridPort;
        startupCmd = String.format("nohup ./startAgent.sh -gridHost=%s -fileServerHost=%s/proxy", subGridUrl, subGridUrl);

        logger.info(String.format("Starting sub-agent with command %s", startupCmd));
        // Start the agent
        execCreateCmdResponse = dockerClient.execCreateCmd(container.getId())
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withWorkingDir("/home/agent/bin/")
                .withCmd("bash", "-c", startupCmd)
                .withUser(CONTAINER_NAME)
                .exec();
        dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .exec(new StringBuilderLogReader(stringBuilder));

        String log = callback.builder.toString();
        logger.info(log);
    }

    private static void copyLocalFolderToContainer(DockerClient dockerClient, CreateContainerResponse container, String folderName) throws IOException {
        String pathToCopy = new File("../" + folderName).getCanonicalPath();
        System.out.println("Path to copy : " + pathToCopy);
        logger.info("Path to copy : " + pathToCopy);

        dockerClient.copyArchiveToContainerCmd(container.getId())
                .withHostResource(pathToCopy)
                .withRemotePath("/home/agent/")
                .exec();
    }

    private void stopContainer(DockerClient dockerClient) {
        dockerClient.stopContainerCmd(CONTAINER_NAME).exec();
        dockerClient.removeContainerCmd(CONTAINER_NAME).exec();
    }

}
