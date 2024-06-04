package step.functions.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.dockerjava.api.DockerClient;
import step.grid.TokenWrapper;
import step.grid.filemanager.FileVersionId;
import step.grid.io.OutputMessage;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

public class DockerContainer implements Closeable, AutoCloseable {

    protected final String containerId;
    protected final TokenWrapper token;
    private final DockerContainerManager dockerContainerManager;
    protected final DockerClient dockerClient;

    protected DockerContainer(DockerContainerManager dockerContainerManager, DockerClient dockerClient, String containerId, TokenWrapper token) {
        this.dockerContainerManager = dockerContainerManager;
        this.dockerClient = dockerClient;
        this.containerId = containerId;
        this.token = token;
    }

    public OutputMessage call(JsonNode input, String messageHandlerClassname, FileVersionId fileVersionId, Map<String, String> messageProperties, int callTimeout) throws Exception {
        return dockerContainerManager.call(this, input, messageHandlerClassname, fileVersionId, messageProperties, callTimeout);
    }

    @Override
    public void close() throws IOException {
        dockerContainerManager.stopContainer(this);
    }
}
