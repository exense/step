package step.functions.handler;

public class DockerContainerManagerConfiguration {

    /**
     * The path or host of the docker socket
     */
    public String dockerSocket;
    /**
     * If running in Kubernetes
     */
    public boolean kubernetesMode;
    /**
     * The maximum time in ms that the container agent should take to start and join the docker grid
     */
    public long containerAgentStartupTimeoutMs = 20_000;

}
