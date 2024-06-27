package step.artefacts.handlers.functions.autoscaler;

public class AgentPoolConstants {
    /**
     * Each provisioning context (which currently corresponds to a plan execution in Step) has a unique ID called partitionId
     * The partitionId is added to the attributes AND selection criteria of all tokens provisioned by this class.
     * This ensures isolation of tokens across provisioning contexts
     */
    public static final String TOKEN_ATTRIBUTE_PARTITION = "$tokenPartition";
}
