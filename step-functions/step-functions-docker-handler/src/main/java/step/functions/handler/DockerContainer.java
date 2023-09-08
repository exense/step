package step.functions.handler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.DockerException;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    // Constant
    public static final String CONTAINER_NAME = "agent";

    private static final Logger logger = LoggerFactory.getLogger(DockerContainer.class);
    private Path startupScriptFilePath;
    private Path configurationFileFilePath;
    private Path logbackConfigurationFilePath;

    {
        // Extracting start script, agent configuration and logback configuration
        Path startupScriptTempFilePath = Paths.get(ResourceExtractor.extractResource(ProxyMessageHandler.class.getClassLoader(), "startAgent.sh").getCanonicalPath());
        Path configurationTempFilePath = Paths.get(ResourceExtractor.extractResource(ProxyMessageHandler.class.getClassLoader(), "AgentConf.yaml").getCanonicalPath());
        Path logbackConfigurationTempFilePath = Paths.get(ResourceExtractor.extractResource(ProxyMessageHandler.class.getClassLoader(), "logback.xml").getCanonicalPath());

        // Creating a copy with a simpler name
        startupScriptFilePath = Paths.get("/tmp/startAgent.sh");
        Files.copy(startupScriptTempFilePath, startupScriptFilePath, StandardCopyOption.REPLACE_EXISTING);
        configurationFileFilePath = Paths.get("/tmp/AgentConf.yaml");
        Files.copy(configurationTempFilePath, configurationFileFilePath, StandardCopyOption.REPLACE_EXISTING);
        logbackConfigurationFilePath = Paths.get("/tmp/logback.xml");
        Files.copy(logbackConfigurationTempFilePath, logbackConfigurationFilePath, StandardCopyOption.REPLACE_EXISTING);
    }

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
        try {
            container = startContainer(dockerImage, containerUser, containerCmd);
            copyAgentMaterialAndStart(dockerInDocker, localGridPort, containerUser);
        } catch (DockerException e) {
            dockerClient.removeContainerCmd(CONTAINER_NAME).withRemoveVolumes(true).withForce(true).exec();
            throw e;
        }

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
                .withName(CONTAINER_NAME)
                .withUser(containerUser)
                .withHostConfig(new HostConfig().withNetworkMode("host"))
                .withCmd(startCmd.split(","))
                .exec();
        dockerClient.startContainerCmd(container.getId()).exec();
        InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(container.getId()).exec();
        logger.info("Container " + CONTAINER_NAME + " startup response : " + inspectContainerResponse.toString());
        return container;
    }

    private void copyAgentMaterialAndStart(boolean dockerInDocker, int gridPort, String containerUser) throws InterruptedException, IOException {
        // Copy agent material to container
        createFolderInContainer(String.format("/home/%s/bin", containerUser));
        createFolderInContainer(String.format("/home/%s/conf", containerUser));
        copyLocalFileToContainer(startupScriptFilePath.toFile(), String.format("/home/%s/bin/", containerUser));
        copyLocalFileToContainer(configurationFileFilePath.toFile(), String.format("/home/%s/conf/", containerUser));
        copyLocalFileToContainer(logbackConfigurationFilePath.toFile(), String.format("/home/%s/bin/", containerUser));
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
        executeContainerCmd(containerUser, String.format("nohup ./startAgent.sh -gridHost=%s -fileServerHost=%s/proxy &", subGridUrl, subGridUrl),
                String.format("/home/%s/bin/", containerUser), false);
    }

    private void executeContainerCmd(String containerUser, String command) throws InterruptedException {
        executeContainerCmd(containerUser, command, null, true);
    }

    private void executeContainerCmd(String containerUser, String command, String workingDir, boolean awaitCompletion) throws InterruptedException {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilderLogReader callback = new StringBuilderLogReader(stringBuilder);
        ExecCreateCmdResponse execCreateCmdResponse;
        String[] bashCommandArray = {"bash", "-c", command};
        ExecCreateCmd builder = dockerClient.execCreateCmd(container.getId())
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withUser(containerUser)
                .withCmd(bashCommandArray);
        if (workingDir != null) {
            builder.withWorkingDir(workingDir);
        }
        execCreateCmdResponse = builder.exec();
        StringBuilderLogReader exec = dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback);
        if (awaitCompletion) {
            exec.awaitCompletion();
            String message = "Executed command '" + String.join(" ", List.of(command)) + "'";
            logger.info(message);
            callback.awaitCompletion(5, TimeUnit.SECONDS);
            logger.info(stringBuilder.toString());
        } else {
            String message = "Started command '" + String.join(" ", List.of(command)) + "'";
            logger.info(message);
            callback.awaitCompletion(5, TimeUnit.SECONDS);
            logger.info(stringBuilder.toString());
        }
    }

    private void copyLocalFileToContainer(File localFile, String remotePath) throws IOException {
        String pathToCopy = localFile.getCanonicalPath();
        logger.info(String.format("Copying local file %s to container %s at path %s", pathToCopy, container.getId(), remotePath));
        dockerClient.copyArchiveToContainerCmd(container.getId())
                .withHostResource(pathToCopy)
                .withRemotePath(remotePath)
                .exec();
    }

    private void createFolderInContainer(String containerFolderPath) throws InterruptedException {
        logger.info(String.format("Creating path %s in container %s", containerFolderPath, container.getId()));
        executeContainerCmd("root", String.format("mkdir -p %s", containerFolderPath));
    }

    private void stopContainer() {
        dockerClient.stopContainerCmd(CONTAINER_NAME).exec();
        dockerClient.removeContainerCmd(CONTAINER_NAME).exec();
    }


    @Override
    public void close() throws IOException {
        stopContainer();
    }
}
