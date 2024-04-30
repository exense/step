package step.artefacts.handlers.functions.autoscaler;

public interface TokenAutoscalingDriver {

    /**
     * @return the configuration of the token autoscaling
     */
    TokenAutoscalingConfiguration getConfiguration();

    /**
     * Token provisioning requests are performed in 2 steps: initialize and execute.
     * The provisioning itself that can take a few minutes should be performed in the execute step.
     * This method corresponds to the initialize step, which is intended to prepare the execution and return quickly.
     *
     * @param request the parameters of the request
     * @return a unique id that identifies the request
     */
    String initializeTokenProvisioningRequest(TokenProvisioningRequest request);

    /**
     * Performs the provisioning request identified by the provided id and
     * previously initialized by initializeTokenProvisioningRequest
     * @param provisioningRequestId the unique id of the request
     */
    void executeTokenProvisioningRequest(String provisioningRequestId);

    /**
     * Returns the status of the provisioning request identified by the provided id
     * @param provisioningRequestId the unique id of the request
     * @return the status of the request or null if the request doesn't exist or completed
     */
    TokenProvisioningStatus getTokenProvisioningStatus(String provisioningRequestId);

    /**
     * Performs the deprovisioning of the token provisioned previously for the provided request id
     * @param provisioningRequestId the unique id of the provisioning request
     */
    void deprovisionTokens(String provisioningRequestId);
}
