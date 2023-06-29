package step.functions.handler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.bootstrap.ResourceExtractor;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

public class DockerContainer implements Closeable {

    private final CreateContainerResponse container;
    private final DockerClient dockerClient;

    // Input message properties
    public static final String MESSAGE_PROP_DOCKER_REGISTRY_URL = "$docker.registryUrl";
    public static final String MESSAGE_PROP_DOCKER_REGISTRY_USERNAME = "$docker.registryUsername";
    public static final String MESSAGE_PROP_DOCKER_REGISTRY_PASSWORD = "$docker.registryPassword";
    public static final String MESSAGE_PROP_DOCKER_IMAGE = "$docker.image";
    public static final String MESSAGE_PROP_CONTAINER_USER = "$container.user";
    public static final String MESSAGE_PROP_CONTAINER_CMD = "$container.cmd";

    // AgentConf properties
    public static final String AGENT_CONF_DOCKER_SOCK = "docker.sock";
    public static final String AGENT_CONF_DOCKER_SOCK_DEFAULT = "unix:///var/run/docker.sock";
    public static final String AGENT_CONF_DOCKER_IN_DOCKER = "docker.in.docker";

    private static final Logger logger = LoggerFactory.getLogger(DockerContainer.class);

    DockerContainer(Map<String, String> agentProperties, Map<String, String> messageProperties, int localGridPort) throws InterruptedException, IOException {
        String dockerSock = agentProperties.getOrDefault(AGENT_CONF_DOCKER_SOCK, AGENT_CONF_DOCKER_SOCK_DEFAULT);
        boolean dockerInDocker = Boolean.parseBoolean(agentProperties.getOrDefault(AGENT_CONF_DOCKER_IN_DOCKER, Boolean.FALSE.toString()));

        String registryUrl = messageProperties.get(MESSAGE_PROP_DOCKER_REGISTRY_URL);
        String registryUsername = messageProperties.get(MESSAGE_PROP_DOCKER_REGISTRY_USERNAME);
        String registryPassword = messageProperties.get(MESSAGE_PROP_DOCKER_REGISTRY_PASSWORD);
        String dockerImage = messageProperties.get(MESSAGE_PROP_DOCKER_IMAGE);
        String containerUser = messageProperties.get(MESSAGE_PROP_CONTAINER_USER);
        String containerCmd = messageProperties.get(MESSAGE_PROP_CONTAINER_CMD);

        dockerClient = initDockerClient(registryUrl, registryUsername, registryPassword, dockerSock);
        container = startContainer(dockerImage, containerUser, containerCmd);
        copyAgentMaterialAndStart(dockerInDocker, localGridPort, containerUser);
    }


    private static DockerClient initDockerClient(String registryUrl, String registryUsername, String registryPassword, String dockerHost) {
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

    private CreateContainerResponse startContainer(String image, String containerUser, String startCmd) throws InterruptedException {
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
        pullImageCmd.exec(new PullImageResultCallback()).awaitCompletion();
        CreateContainerResponse container = dockerClient.createContainerCmd(image)
                .withName(ProxyMessageHandler.CONTAINER_NAME)
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

    private void copyAgentMaterialAndStart(boolean dockerInDocker, int gridPort, String containerUser) throws InterruptedException, IOException {
        // Extracting start script and agent configuration
        Path startupScriptTempFilePath = Paths.get(ResourceExtractor.extractResource(ProxyMessageHandler.class.getClassLoader(), "startAgent.sh").getCanonicalPath());
        Path configurationTempFilePath = Paths.get(ResourceExtractor.extractResource(ProxyMessageHandler.class.getClassLoader(), "AgentConf.yaml").getCanonicalPath());
        // Creating a copy with a simpler name
        Path startupScriptFilePath = Paths.get("/tmp/startAgent.sh");
        Files.copy(startupScriptTempFilePath, startupScriptFilePath, StandardCopyOption.REPLACE_EXISTING);
        Path configurationFileFilePath = Paths.get("/tmp/AgentConf.yaml");
        Files.copy(configurationTempFilePath, configurationFileFilePath, StandardCopyOption.REPLACE_EXISTING);

        createFolderInContainer(String.format("/home/%s/bin", containerUser));
        createFolderInContainer(String.format("/home/%s/conf", containerUser));
        copyLocalFileToContainer(startupScriptFilePath.toFile(), String.format("/home/%s/bin/", containerUser));
        copyLocalFileToContainer(configurationFileFilePath.toFile(), String.format("/home/%s/conf/", containerUser));
        copyLocalFileToContainer(new File("../lib"), String.format("/home/%s/", containerUser));

        // Files are copied as root, we need to change the ownership
        executeContainerCmd("root", String.format("chown -R %s:%s /home/%s", containerUser, containerUser, containerUser));

        // Make the startupScript executable
        executeContainerCmd(containerUser, String.format("chmod +x /home/%s/bin/startAgent.sh", containerUser));

        // Start the agent
        String gridHost;
        if (dockerInDocker) {
            gridHost = System.getenv("POD_IP");
        } else {
            gridHost = "localhost";
        }
        String subGridUrl = "http://" + gridHost + ":" + gridPort;
        executeContainerCmd(containerUser, String.format("nohup ./startAgent.sh -gridHost=%s -fileServerHost=%s/proxy", subGridUrl, subGridUrl));
    }

    private void executeContainerCmd(String containerUser, String command) throws InterruptedException {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilderLogReader callback = new StringBuilderLogReader(stringBuilder);
        ExecCreateCmdResponse execCreateCmdResponse;
        logger.debug("Making the startup script executable");
        String[] bashCommandArray = {"bash", "-c", command};
        execCreateCmdResponse = dockerClient.execCreateCmd(container.getId())
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withUser(containerUser)
                .withCmd(bashCommandArray)
                .exec();
        dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .exec(new StringBuilderLogReader(stringBuilder))
                .awaitCompletion();
        logger.debug("Executed command '" + String.join(" ", Arrays.asList(command)) + "'. Output: " + callback.builder.toString());
    }

    private void copyLocalFileToContainer(File localFile, String remotePath) throws IOException {
        String pathToCopy = localFile.getCanonicalPath();
        logger.debug("Copying local file %s to container %s at path %s%n", pathToCopy, container.getId(), remotePath);
        dockerClient.copyArchiveToContainerCmd(container.getId())
                .withHostResource(pathToCopy)
                .withRemotePath(remotePath)
                .exec();
    }

    private void createFolderInContainer(String containerFolderPath) throws InterruptedException {
        logger.debug("Creating path %s in container %s%n", containerFolderPath, container.getId());
        executeContainerCmd("root", String.format("mkdir -p %s", containerFolderPath));
    }

    private void stopContainer() {
        dockerClient.stopContainerCmd(ProxyMessageHandler.CONTAINER_NAME).exec();
        dockerClient.removeContainerCmd(ProxyMessageHandler.CONTAINER_NAME).exec();
    }


    @Override
    public void close() throws IOException {
        stopContainer();
    }
}
