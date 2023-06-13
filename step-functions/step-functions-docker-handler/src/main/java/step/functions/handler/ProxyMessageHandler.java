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
import java.time.Duration;
import java.util.Map;

public class ProxyMessageHandler implements MessageHandler {

    public static final String MESSAGE_HANDLER = "$proxy.messageHandler";
    public static final String MESSAGE_HANDLER_FILE_ID = "$proxy.messageHandler.file.id";
    public static final String MESSAGE_HANDLER_FILE_VERSION = "$proxy.messageHandler.file.version";
    private static final Logger logger = LoggerFactory.getLogger(ProxyMessageHandler.class);

    @Override
    public OutputMessage handle(AgentTokenWrapper agentTokenWrapper, InputMessage inputMessage) throws Exception {
        // TODO below settings location to be brainstormed (session routing, parameters, properties ?)
        boolean dockerInDocker = agentTokenWrapper.getProperties().containsKey("$docker.in.docker");
        logger.info("Docker in docker configuration detected : " + dockerInDocker);
        String containerName = "agent";
        DockerClient dockerClient = initDockerClient("https://docker.exense.ch", "docker-user", "100%BuildPROD", "unix:///var/run/docker.sock");
        CreateContainerResponse container = startContainer(dockerClient, containerName, "docker.exense.ch/base/agent:11.0.13-jre-slim");
        copyAgentMaterialAndStart(dockerClient, container, dockerInDocker);

        FileManagerClient fileManagerClient = agentTokenWrapper.getServices().getFileManagerClient();
        ProxyGridServices.fileManagerClient = fileManagerClient;

        // Get principal Agent properties
        //agentTokenWrapper.getProperties();

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

            String messageHandler = inputMessage.getProperties().get(MESSAGE_HANDLER);
            String messageHandlerFileId = inputMessage.getProperties().get(MESSAGE_HANDLER_FILE_ID);
            String messageHandlerFileVersion = inputMessage.getProperties().get(MESSAGE_HANDLER_FILE_VERSION);
            FileVersionId messageHandlerFileVersionId = new FileVersionId(messageHandlerFileId, messageHandlerFileVersion);

            // Execute a keyword using the selected token
            OutputMessage outputMessage = gridClient.call(tokenHandle.getID(), inputMessage.getPayload(), messageHandler, messageHandlerFileVersionId, inputMessage.getProperties(), 60000);
            return outputMessage;
        } finally {
            // Stop the grid
            grid.stop();
            stopContainer(dockerClient, containerName);
        }
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

    private CreateContainerResponse startContainer(DockerClient dockerClient, String name, String image) throws InterruptedException {
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
        pullImageCmd.exec(new PullImageResultCallback()).awaitCompletion();
        CreateContainerResponse container = dockerClient.createContainerCmd(image)
                .withName(name)
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

    private void copyAgentMaterialAndStart(DockerClient dockerClient, CreateContainerResponse container, boolean dockerInDocker) throws InterruptedException {
        StringBuilder stringBuilder = new StringBuilder();
        final StringBuilderLogReader callback = new StringBuilderLogReader(stringBuilder);

        // TODO find a way to deploy agent : package from existing, download from somewhere ? For testing purpose agent folder is available on the test VM
        dockerClient.copyArchiveToContainerCmd(container.getId())
                .withHostResource("/tmp/step-enterprise-agent")
                .withRemotePath("/home/agent/")
                .exec();

        // Files are copied as root, we need to change the ownership
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(container.getId())
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withUser("root")
                .withWorkingDir("/home/agent/")
                .withCmd("bash", "-c", "chown -R agent:agent step-enterprise-agent")
                .exec();
        dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .exec(new StringBuilderLogReader(stringBuilder))
                .awaitCompletion();

        String startupCmd = dockerInDocker ? "nohup ./startAgent.sh -gridHost=http://${POD_IP}:8090 -fileServerHost=http://${POD_IP}:8090/proxy" : "nohup ./startAgent.sh &";
        logger.info(String.format("Starting sub-agent with command %s", startupCmd));
        // Start the agent
        execCreateCmdResponse = dockerClient.execCreateCmd(container.getId())
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withWorkingDir("/home/agent/step-enterprise-agent/bin/")
                .withCmd("bash", "-c", startupCmd)
                .withUser("agent")
                .exec();
        dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .exec(new StringBuilderLogReader(stringBuilder));

        String log = callback.builder.toString();
        logger.info(log);
    }

    private void stopContainer(DockerClient dockerClient, String name) {
        dockerClient.stopContainerCmd(name).exec();
        dockerClient.removeContainerCmd(name).exec();
    }

}
