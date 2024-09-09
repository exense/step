package step.artefacts.handlers.functions.autoscaler;

import step.grid.tokenpool.Interest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AgentPoolProvisioningParameters {

    public static final String TOKEN_ATTRIBUTE_DOCKER_IMAGE = "$dockerImage";
    public static final String TOKEN_ATTRIBUTE_DOCKER_SUPPORT = "$supportsCustomDockerImage";
    public static final String PROVISIONING_PARAMETER_DOCKER_IMAGE = "dockerImage";

    public static final AgentPoolProvisioningParameter DOCKER_IMAGE = new AgentPoolProvisioningParameter(PROVISIONING_PARAMETER_DOCKER_IMAGE, "Docker image", (criteria, provisioningParameters) -> {
        if(criteria.containsKey(TOKEN_ATTRIBUTE_DOCKER_IMAGE)) {
            provisioningParameters.put(PROVISIONING_PARAMETER_DOCKER_IMAGE, criteria.get(TOKEN_ATTRIBUTE_DOCKER_IMAGE).getSelectionPattern().pattern());
        }
    }, criteria -> {
        if (criteria.getKey().equals(TOKEN_ATTRIBUTE_DOCKER_IMAGE)) {
            return Map.entry(TOKEN_ATTRIBUTE_DOCKER_SUPPORT, new Interest(Pattern.compile(Boolean.TRUE.toString()), true));
        } else {
            return null;
        }
    });

    public static final List<AgentPoolProvisioningParameter> supportedParameters = List.of(DOCKER_IMAGE);
}
