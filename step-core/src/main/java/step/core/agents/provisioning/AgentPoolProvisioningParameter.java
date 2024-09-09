package step.core.agents.provisioning;

import com.fasterxml.jackson.annotation.JsonIgnore;
import step.grid.tokenpool.Interest;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class AgentPoolProvisioningParameter {

    public String name;
    public String label;
    @JsonIgnore
    public BiConsumer<Map<String, Interest>, Map<String, String>> tokenSelectionCriteriaToAgentPoolProvisioningParameters;
    @JsonIgnore
    public Function<Map.Entry<String, Interest>, Map.Entry<String, Interest>> preProvisioningTokenSelectionCriteriaTransformer;

    public AgentPoolProvisioningParameter() {
    }

    /**
     * @param name the unique name of the provisioning parameter
     * @param label the label of the provisioning parameter to be displayed to the users
     * @param tokenSelectionCriteriaToAgentPoolProvisioningParameters defines the consumer that creates the provisioning parameters based on the selection criteria
     * @param preProvisioningTokenSelectionCriteriaTransformer defines the transformer of token selection criteria.
     *                                                         Some token attributes are only populated at provisioning time and have to be either removed
     *                                                         or transformed during the matching of the configured agent pool templates. This is the role of this transformer
     */
    public AgentPoolProvisioningParameter(String name, String label, BiConsumer<Map<String, Interest>,
            Map<String, String>> tokenSelectionCriteriaToAgentPoolProvisioningParameters, Function<Map.Entry<String, Interest>, Map.Entry<String, Interest>> preProvisioningTokenSelectionCriteriaTransformer) {
        this.name = name;
        this.label = label;
        this.tokenSelectionCriteriaToAgentPoolProvisioningParameters = tokenSelectionCriteriaToAgentPoolProvisioningParameters;
        this.preProvisioningTokenSelectionCriteriaTransformer = preProvisioningTokenSelectionCriteriaTransformer;
    }
}
