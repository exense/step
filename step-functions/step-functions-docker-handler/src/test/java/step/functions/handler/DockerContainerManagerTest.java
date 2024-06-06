package step.functions.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Ignore;
import org.junit.Test;
import step.grid.bootstrap.ResourceExtractor;
import step.grid.filemanager.FileVersion;
import step.grid.io.Attachment;
import step.grid.io.OutputMessage;

import java.io.File;
import java.util.Base64;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DockerContainerManagerTest {

    @Ignore // Ignoring for the moment as docker isn't installed on our build agents
    @Test
    public void newContainer() throws Exception {
        DockerContainerManagerConfiguration configuration = new DockerContainerManagerConfiguration();
        configuration.dockerSocket = isWindows() ? "tcp://localhost:2375" : "unix:///var/run/docker.sock";
        configuration.kubernetesMode = false;
        configuration.containerAgentStartupTimeoutMs = 120_000;

        DockerRegistry dockerRegistry = new DockerRegistry();
        dockerRegistry.registryUrl = "docker.io";

        File testJar = ResourceExtractor.extractResource(this.getClass().getClassLoader(), "step-functions-docker-handler-test.jar");
        File agentLib = ResourceExtractor.extractResource(this.getClass().getClassLoader(), "agentlibs/step-grid-agent.jar");

        DockerContainerManager dockerContainerManager = new DockerContainerManager(configuration, null, agentLib);

        FileVersion fileVersion = dockerContainerManager.getGrid().registerFile(testJar, true);

        try (DockerContainer dockerContainer = dockerContainerManager.newContainer(dockerRegistry, "docker.io/openjdk:latest", "root", "bash,-c,sleep infinity")) {
            ObjectNode input = new ObjectMapper().createObjectNode().put("MyKey", "MyValue");
            OutputMessage outputMessage = dockerContainer.call(input, TestMessageHandler.class.getName(), fileVersion.getVersionId(), Map.of(), 1000);
            assertNull(outputMessage.getAttachments());
            if (outputMessage.getAttachments() != null) {
                outputMessage.getAttachments().forEach(this::printAttachmentContent);
            }
            assertEquals(input, outputMessage.getPayload());
        }
    }

    private void printAttachmentContent(Attachment attachment) {
        System.out.println(new String(Base64.getDecoder().decode(attachment.getHexContent())));
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}