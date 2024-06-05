package step.functions.handler;

import ch.exense.commons.io.FileHelper;
import ch.exense.commons.io.Poller;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.GridImpl;
import step.grid.ProxyGridServices;
import step.grid.TokenWrapper;
import step.grid.client.*;
import step.grid.filemanager.*;
import step.grid.io.OutputMessage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class DockerContainerManager {

    private static final Logger logger = LoggerFactory.getLogger(DockerContainerManager.class);
    private static final long CONTAINER_STARTUP_TIMEOUT_MS = 120_000;
    private static final String CONTAINER_AGENT_INTERNAL_PORT = "8082";
    public static final String DEFAULT_CONTAINER_CMD = "bash,-c,sleep infinity";
    public static final String DEFAULT_CONTAINER_USER = "root";
    private final Path startupScriptFilePath;
    private final Path configurationFileFilePath;
    private final Path logbackConfigurationFilePath;
    private final Map<String, DockerClient> dockerClients = new ConcurrentHashMap<>();
    private final GridImpl grid;
    private final GridClient gridClient;
    private final boolean inK8s;
    private final int localGridPort;
    private final File gridLibs;
    private final String dockerSock;

    /**
     * @param configuration the configuration to be used for this instance
     * @param fileManagerClient the {@link FileManagerClient} that should be used to serve incoming file manager request from the sub agents of the Docker container.
     *                          If null the file manager of the local grid of this class will be user
     * @param gridLibs the path to the lib folder containing the libs of the agent to be used to start the sub agent within the Docker containers.
     * @throws IOException
     */
    public DockerContainerManager(DockerContainerManagerConfiguration configuration, FileManagerClient fileManagerClient, File gridLibs) throws IOException {
        dockerSock = configuration.dockerSocket;
        inK8s = configuration.kubernetesMode;
        // Start grid
        grid = createLocalGrid(fileManagerClient);
        gridClient = createLocalGridClient();
        localGridPort = grid.getServerPort();

        String agentStartScript;
        if(gridLibs.isDirectory()) {
            // If the gridLibs is a directory we assume it points to the lib folder of an agent distribution.
            // In this case we use the standard start script of the agent
            agentStartScript = "startAgent.sh";
            this.gridLibs = gridLibs;
        } else {
            // If the gridLibs is a single file we assume it points to the uber jar of the agent distribution.
            // In this case we have to use a special start script of the agent with a different main class
            agentStartScript = "startAgentLocal.sh";
            File agentLibFolder = FileHelper.createTempFolder();
            Files.move(gridLibs.toPath(), agentLibFolder.toPath().resolve(gridLibs.getName()));
            this.gridLibs = agentLibFolder;
        }

        // Read the agent configuration files from the CL and cache them on the filesystem
        File tempFolder = FileHelper.createTempFolder();
        startupScriptFilePath = writeResourceToTempFolder(tempFolder, agentStartScript, "startAgent.sh");
        configurationFileFilePath = writeResourceToTempFolder(tempFolder, "AgentConf.yaml");
        logbackConfigurationFilePath = writeResourceToTempFolder(tempFolder, "logback.xml");
    }

    private GridClient createLocalGridClient() {
        GridClientConfiguration gridClientConfiguration = new GridClientConfiguration();
        // Configure the selection timeout (this should be higher than the start time of the container)
        gridClientConfiguration.setNoMatchExistsTimeout(CONTAINER_STARTUP_TIMEOUT_MS);
        return new LocalGridClientImpl(gridClientConfiguration, grid);
    }

    private static Path writeResourceToTempFolder(File tempFolder, String resourceName) throws IOException {
        return writeResourceToTempFolder(tempFolder, resourceName, resourceName);
    }

    private static Path writeResourceToTempFolder(File tempFolder, String resourceName, String targetFilename) throws IOException {
        Path path = tempFolder.toPath().resolve(targetFilename);
        Files.writeString(path, readResource(resourceName));
        return path;
    }

    private static String readResource(String name) throws IOException {
        try (InputStream resourceAsStream = DockerMessageHandler.class.getClassLoader().getResourceAsStream(name)) {
            assert resourceAsStream != null;
            // Normalize line ending (clrf to lf) in as clrf cause issues in the bash scripts within the container
            return new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\r", "\n");
        }
    }

    private DockerClient getDockerClientForRegistry(DockerRegistry dockerRegistry) {
        return dockerClients.computeIfAbsent(dockerRegistry.registryUrl, registryUrl -> {
            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withRegistryUrl(dockerRegistry.registryUrl)
                    .withRegistryUsername(dockerRegistry.registryUsername)
                    .withRegistryPassword(dockerRegistry.registryPassword)
                    .withDockerHost(dockerSock)
                    .build();
            ApacheDockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(100)
                    .connectionTimeout(Duration.ofSeconds(30))
                    .responseTimeout(Duration.ofSeconds(45))
                    .build();
            return DockerClientImpl.getInstance(config, dockerHttpClient);
        });
    }

    private static GridImpl createLocalGrid(FileManagerClient fileManagerClient) {
        logger.info("Starting local grid...");
        GridImpl grid = new GridImpl(new File("./filemanager"), 0);
        try {
            grid.start();
        } catch (Exception e) {
            throw new RuntimeException("Error while starting local grid", e);
        }
        logger.info("Local grid started on port " + grid.getServerPort());
        if(fileManagerClient == null) {
            FileManagerClient localFileManagerClient = getDelegatingFileManagerClient(grid);
            ProxyGridServices.fileManagerClient = localFileManagerClient;
        } else {
            ProxyGridServices.fileManagerClient = fileManagerClient;
        }
        return grid;
    }

    private static FileManagerClient getDelegatingFileManagerClient(GridImpl grid) {
        return new FileManagerClient() {

            @Override
            public void close() {

            }

            @Override
            public FileVersion requestFileVersion(FileVersionId fileVersionId, boolean b) throws FileManagerException {
                return grid.getRegisteredFile(fileVersionId);
            }

            @Override
            public void removeFileVersionFromCache(FileVersionId fileVersionId) {
                grid.unregisterFile(fileVersionId);
            }

            @Override
            public void cleanupCache() {
                grid.cleanupFileManagerCache();
            }
        };
    }

    /**
     * Start a new container
     * @param dockerRegistry the configuration of the docker registry to be used to pull the specified image
     * @param dockerImage the docker image to be used (ex: docker.io/openjdk:latest)
     * @param containerUser the user to be used to start the sub agent within the container. If null, 'root' is used per default
     * @param containerCmd
     * @return
     * @throws Exception
     */
    public DockerContainer newContainer(DockerRegistry dockerRegistry, String dockerImage, String containerUser, String containerCmd) throws Exception {
        containerUser = containerUser != null && !containerUser.isEmpty() ? containerUser : DEFAULT_CONTAINER_USER;
        containerCmd = containerCmd != null && !containerCmd.isEmpty() ? containerCmd : DEFAULT_CONTAINER_CMD;

        DockerClient dockerClient = getDockerClientForRegistry(dockerRegistry);

        StartContainerResponse startContainerResponse = startContainer(dockerClient, dockerImage, containerUser, containerCmd);
        String containerId = startContainerResponse.containerId;
        try {
            copyAgentMaterialAndStart(dockerClient, containerId, containerUser, startContainerResponse.agentPort);
            logger.info("Waiting for agent to connect...");
            TokenWrapper token;
            // Wait for the token to be available i.e. that the container started
            try {
                token = gridClient.getTokenHandle(Map.of(), Map.of(), true);
            } catch (Exception e) {
                throw new RuntimeException("Error while waiting for container to connect to the Grid. An error might have occurred during the container startup or the startup took longer than the configured timeout", e);
            }
            logger.info("Container ready to execute commands");
            return new DockerContainer(this, dockerClient, containerId, token);
        } catch (Exception e) {
            logger.error("Error while creating docker container", e);
            removeContainer(dockerClient, containerId);
            throw e;
        }
    }

    private StartContainerResponse startContainer(DockerClient dockerClient, String image, String containerUser, String startCmd) throws InterruptedException, TimeoutException {
        logger.info("Pulling image " + image + "...");
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
        pullImageCmd.exec(new PullImageResultCallback()).awaitCompletion();
        logger.info("Starting container...");
        CreateContainerResponse container = dockerClient.createContainerCmd(image)
                .withUser(containerUser)
                .withHostConfig(new HostConfig().withPortBindings(PortBinding.parse(CONTAINER_AGENT_INTERNAL_PORT)))
                .withExposedPorts(ExposedPort.parse(CONTAINER_AGENT_INTERNAL_PORT))
                .withCmd(startCmd.split(","))
                .exec();
        dockerClient.startContainerCmd(container.getId()).exec();

        AtomicReference<InspectContainerResponse> inspectContainerResponse = new AtomicReference<>();
        Poller.waitFor(() -> {
            inspectContainerResponse.set(dockerClient.inspectContainerCmd(container.getId()).exec());
            return inspectContainerResponse.get().getNetworkSettings().getPorts().getBindings().size() > 0;
        }, 1000, 50);

        String agentPort = Arrays.stream(inspectContainerResponse.get().getNetworkSettings().getPorts().getBindings().get(ExposedPort.parse(CONTAINER_AGENT_INTERNAL_PORT + "/tcp"))).findFirst().orElseThrow().getHostPortSpec();
        String agentIp = inspectContainerResponse.get().getNetworkSettings().getIpAddress();

        logger.debug("Container startup response : " + inspectContainerResponse);
        return new StartContainerResponse(container.getId(), agentIp, agentPort);
    }

    private void copyAgentMaterialAndStart(DockerClient dockerClient, String containerId, String containerUser, String agentPort) throws InterruptedException, IOException {
        logger.info("Copying agent to container...");
        // Copy agent material to container
        createFolderInContainer(dockerClient, containerId, String.format("/home/%s/agent/bin", containerUser));
        createFolderInContainer(dockerClient, containerId, String.format("/home/%s/agent/conf", containerUser));
        createFolderInContainer(dockerClient, containerId, String.format("/home/%s/agent/lib", containerUser));
        copyLocalFileToContainer(dockerClient, containerId, startupScriptFilePath.toFile(), String.format("/home/%s/agent/bin/", containerUser));
        copyLocalFileToContainer(dockerClient, containerId, configurationFileFilePath.toFile(), String.format("/home/%s/agent/conf/", containerUser));
        copyLocalFileToContainer(dockerClient, containerId, logbackConfigurationFilePath.toFile(), String.format("/home/%s/agent/bin/", containerUser));
        copyLocalFileToContainer(dockerClient, containerId, gridLibs, String.format("/home/%s/agent/lib/", containerUser));
        // Files are copied as root, we need to change the ownership
        executeContainerCmd(dockerClient, containerId, DEFAULT_CONTAINER_USER, String.format("chown -R %s:%s /home/%s/agent/", containerUser, containerUser, containerUser));
        // Make the startupScript executable
        executeContainerCmd(dockerClient, containerId, containerUser, String.format("chmod +x /home/%s/agent/bin/startAgent.sh", containerUser));

        // Start the agent
        String gridHost;
        if (inK8s) {
            gridHost = System.getenv("POD_IP");
        } else {
            // The following requires port forwarding to work with WSL2:
            // netsh interface portproxy add v4tov4 listenport="8090" connectaddress="172.21.26.204" connectport="8090"
            gridHost = "host.docker.internal"; //getIPAddressOfEth0(); //"host.docker.internal" ; //localhost";
        }
        logger.info("Starting agent in container...");
        String subGridUrl = "http://" + gridHost + ":" + localGridPort;
        String command = String.format("./startAgent.sh -agentUrl=http://localhost:%s -agentHost=localhost -agentPort=%s -gridHost=%s -fileServerHost=%s/proxy >> subagent.out 2>&1 &", agentPort, CONTAINER_AGENT_INTERNAL_PORT, subGridUrl, subGridUrl);
        executeContainerCmd(dockerClient, containerId, containerUser, command, String.format("/home/%s/agent/bin/", containerUser));
    }

    private void executeContainerCmd(DockerClient dockerClient, String containerId, String containerUser, String command) throws InterruptedException {
        executeContainerCmd(dockerClient, containerId, containerUser, command, null);
    }

    private void executeContainerCmd(DockerClient dockerClient, String containerId, String containerUser, String command, String workingDir) throws InterruptedException {
        String commandStr = String.join(" ", List.of(command));
        logger.debug("Executing command '" + commandStr + "'...");

        StringBuilder stringBuilder = new StringBuilder();
        StringBuilderLogReader callback = new StringBuilderLogReader(stringBuilder);
        ExecCreateCmdResponse execCreateCmdResponse;
        String[] bashCommandArray = {"bash", "-c", command};
        ExecCreateCmd builder = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withUser(containerUser)
                .withCmd(bashCommandArray);
        if (workingDir != null) {
            builder.withWorkingDir(workingDir);
        }
        execCreateCmdResponse = builder.exec();
        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback);
        logger.debug("Executed command '" + commandStr + "'");
        callback.awaitCompletion(5, TimeUnit.SECONDS);
        logger.debug(stringBuilder.toString());
    }

    private void copyLocalFileToContainer(DockerClient dockerClient, String containerId, File localFile, String remotePath) throws IOException {
        String pathToCopy = localFile.getCanonicalPath();
        if(localFile.isDirectory()) {
            pathToCopy = pathToCopy + "/.";
        }
        logger.debug(String.format("Copying local file %s to container %s at path %s", pathToCopy, containerId, remotePath));
        dockerClient.copyArchiveToContainerCmd(containerId)
                .withHostResource(pathToCopy)
                .withRemotePath(remotePath)
                .exec();
    }

    private void createFolderInContainer(DockerClient dockerClient, String containerId, String containerFolderPath) throws InterruptedException {
        logger.debug(String.format("Creating path %s in container %s", containerFolderPath, containerId));
        executeContainerCmd(dockerClient, containerId, DEFAULT_CONTAINER_USER, String.format("mkdir -p %s", containerFolderPath));
    }

    public GridImpl getGrid() {
        return grid;
    }

    public OutputMessage call(DockerContainer container, JsonNode jsonNode, String s1, FileVersionId fileVersionId, Map<String, String> map, int i) throws Exception {
        logger.info("Calling token...");
        return gridClient.call(container.token.getID(), jsonNode, s1, fileVersionId, map, i);
    }

    public void stopContainer(DockerContainer container) {
        returnAndInvalidateToken(container);
        removeContainer(container.dockerClient, container.containerId);
    }

    private void returnAndInvalidateToken(DockerContainer container) {
        TokenWrapper token = container.token;
        logger.info("Returning token...");
        try {
            gridClient.returnTokenHandle(token.getID());
        } catch (GridClientException e) {
            logger.warn("Error while returning token to docker local grid", e);
        } catch (AbstractGridClientImpl.AgentCommunicationException e) {
            throw new RuntimeException(e);
        }
        logger.info("Invalidating token...");
        grid.invalidateToken(token.getID());
    }

    private void removeContainer(DockerClient dockerClient, String containerId) {
        logger.info("Removing container...");
        if (dockerClient != null) {
            dockerClient.removeContainerCmd(containerId).withRemoveVolumes(true).withForce(true).exec();
            logger.info("Container removed");
        }
    }

    private static class StartContainerResponse {
        public final String containerId;
        public final String agentIp;
        public final String agentPort;

        public StartContainerResponse(String id, String agentIp, String agentPort) {
            this.containerId = id;
            this.agentPort = agentPort;
            this.agentIp = agentIp;
        }
    }
}
