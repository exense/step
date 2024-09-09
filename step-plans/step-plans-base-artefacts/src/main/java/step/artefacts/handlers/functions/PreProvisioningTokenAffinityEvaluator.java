package step.artefacts.handlers.functions;

import step.artefacts.handlers.functions.autoscaler.AgentPoolProvisioningParameters;
import step.grid.TokenPretender;
import step.grid.tokenpool.Identity;
import step.grid.tokenpool.Interest;
import step.grid.tokenpool.SimpleAffinityEvaluator;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static step.artefacts.handlers.functions.autoscaler.AgentPoolProvisioningParameters.TOKEN_ATTRIBUTE_DOCKER_IMAGE;

/**
 * This {@link step.grid.tokenpool.AffinityEvaluator} is used to match agent token pools before their provisioning.
 * The reason for this is that some attributes are only available after provisioning. An example is the attribute
 * $dockerImage which is only populated after provisioning of the agent pool
 */
public class PreProvisioningTokenAffinityEvaluator extends SimpleAffinityEvaluator {
    @Override
    public int getAffinityScore(Identity i1, Identity i2) {
        return super.getAffinityScore(replaceCriteria(i1), replaceCriteria(i2));
    }

    private static TokenPretender replaceCriteria(Identity i1) {
        // Delegate the transformation of the selection criteria to the registered agent pool provisioning parameters. The first non-null transformation is returned
        Map<String, Interest> newInterests = i1.getInterests().entrySet().stream().map(e -> AgentPoolProvisioningParameters.supportedParameters.stream()
                .map(p -> p.preProvisioningTokenSelectionCriteriaTransformer.apply(e)).filter(Objects::nonNull).findFirst().orElse(e)).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        TokenPretender changedI1 = new TokenPretender(i1.getAttributes(), newInterests);
        return changedI1;
    }
}
