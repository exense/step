package step.functions.handler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.DockerContextMetaFile;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.GridImpl;
import step.grid.ProxyGridServices;
import step.grid.TokenWrapper;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.bootstrap.ResourceExtractor;
import step.grid.client.GridClientConfiguration;
import step.grid.client.LocalGridClientImpl;
import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileVersionId;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    public static final String MESSAGE_PROP_CONTAINER_USER = "$container.user";
    public static final String MESSAGE_PROP_CONTAINER_CMD = "$container.cmd";

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
        String containerUser = messageProperties.get(MESSAGE_PROP_CONTAINER_USER);
        String containerCmd = messageProperties.get(MESSAGE_PROP_CONTAINER_CMD);
        String localGridPortStr = agentProperties.getOrDefault(AGENT_CONF_DOCKER_LOCALGRID_PORT, "8090");
        int localGridPort = Integer.parseInt(localGridPortStr);
        boolean dockerInDocker = agentProperties.containsKey(AGENT_CONF_DOCKER_IN_DOCKER);

        logger.info("Docker in docker configuration detected : " + dockerInDocker);

        DockerClient dockerClient = initDockerClient(registryUrl, registryUsername, registryPassword, dockerHost);
        CreateContainerResponse container = startContainer(dockerClient, dockerImage, containerUser, containerCmd);
        copyAgentMaterialAndStart(dockerClient, container, dockerInDocker, localGridPort, containerUser);

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

    private CreateContainerResponse startContainer(DockerClient dockerClient, String image, String containerUser, String startCmd) throws InterruptedException {
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
        pullImageCmd.exec(new PullImageResultCallback()).awaitCompletion();
        CreateContainerResponse container = dockerClient.createContainerCmd(image)
                .withName(CONTAINER_NAME)
                .withUser(containerUser)
                //.withExposedPorts(new ExposedPort(input.getInt("exposedPort")))
                //.withPortBindings(PortBinding.parse(input.getString("portBindings")))
                .withHostConfig(new HostConfig().withNetworkMode("host")/*.withPortBindings(new PortBinding(Ports.Binding.bindPort(30000), ExposedPort.tcp(30000)))*/)
                // Command used only for testing
                //.withCmd("bash", "-c", "sleep 300")
                .withCmd(startCmd.split(","))
                .exec();
        dockerClient.startContainerCmd(container.getId()).exec();
        InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(container.getId()).exec();
        logger.info("Container startup response : " + inspectContainerResponse.toString());
        return container;
    }

    private void copyAgentMaterialAndStart(DockerClient dockerClient, CreateContainerResponse container, boolean dockerInDocker, int gridPort, String containerUser) throws InterruptedException, IOException {
        StringBuilder stringBuilder = new StringBuilder();
        final StringBuilderLogReader callback = new StringBuilderLogReader(stringBuilder);

        Path startupScriptTempFilePath = Paths.get(ResourceExtractor.extractResource(ProxyMessageHandler.class.getClassLoader(), "startAgent.sh").getCanonicalPath());
        Path configurationTempFilePath = Paths.get(ResourceExtractor.extractResource(ProxyMessageHandler.class.getClassLoader(), "AgentConf.yaml").getCanonicalPath());
        // Creating a copy with a simpler name
        Path startupScriptFilePath = Paths.get("/tmp/startAgent.sh");
        Files.copy(startupScriptTempFilePath, startupScriptFilePath, StandardCopyOption.REPLACE_EXISTING);
        Path configurationFileFilePath = Paths.get("/tmp/AgentConf.yaml");
        Files.copy(configurationTempFilePath, configurationFileFilePath, StandardCopyOption.REPLACE_EXISTING);

        createFolderInContainer(dockerClient, container, String.format("/home/%s/bin", containerUser));
        createFolderInContainer(dockerClient, container, String.format("/home/%s/conf", containerUser));
        copyLocalFileToContainer(dockerClient, container, startupScriptFilePath.toFile(), String.format("/home/%s/bin/", containerUser));
        copyLocalFileToContainer(dockerClient, container, configurationFileFilePath.toFile(), String.format("/home/%s/conf/", containerUser));
        copyLocalFolderToContainer(dockerClient, container, "lib", containerUser);

        // Files are copied as root, we need to change the ownership
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(container.getId())
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withUser("root")
                .withCmd("bash", "-c", String.format("chown -R %s:%s /home/%s", containerUser, containerUser, containerUser))
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
                .withWorkingDir(String.format("/home/%s/bin/", containerUser))
                .withCmd("bash", "-c", startupCmd)
                .withUser(CONTAINER_NAME)
                .exec();
        dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .exec(new StringBuilderLogReader(stringBuilder));

        String log = callback.builder.toString();
        logger.info(log);
    }

    private static void copyLocalFileToContainer(DockerClient dockerClient, CreateContainerResponse container, File file, String containerPath) throws IOException {
        String fileToCopy = file.getCanonicalPath();
        System.out.printf("Copying file %s to container %s at path %s%n", fileToCopy, container.getId(), containerPath);
        dockerClient.copyArchiveToContainerCmd(container.getId())
                .withHostResource(fileToCopy)
                .withRemotePath(containerPath)
                .exec();
    }

    private static void copyLocalFolderToContainer(DockerClient dockerClient, CreateContainerResponse container, String folderName, String containerUser) throws IOException {
        String pathToCopy = new File("../" + folderName).getCanonicalPath();
        System.out.println("Path to copy : " + pathToCopy);
        logger.info("Path to copy : " + pathToCopy);

        dockerClient.copyArchiveToContainerCmd(container.getId())
                .withHostResource(pathToCopy)
                .withRemotePath(String.format("/home/%s/", containerUser))
                .exec();
    }

    private static void createFolderInContainer(DockerClient dockerClient, CreateContainerResponse container, String containerFolderPath) throws InterruptedException {
        System.out.printf("Creating path %s in container %s%n", containerFolderPath, container.getId());
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(container.getId())
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withUser("root")
                .withCmd("bash", "-c", String.format("mkdir -p %s", containerFolderPath))
                .exec();
        dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .exec(new StringBuilderLogReader(new StringBuilder()))
                .awaitCompletion();
    }

    private void stopContainer(DockerClient dockerClient) {
        dockerClient.stopContainerCmd(CONTAINER_NAME).exec();
        dockerClient.removeContainerCmd(CONTAINER_NAME).exec();
    }

}
