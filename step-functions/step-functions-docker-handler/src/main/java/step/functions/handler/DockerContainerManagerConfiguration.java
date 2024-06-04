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

}
