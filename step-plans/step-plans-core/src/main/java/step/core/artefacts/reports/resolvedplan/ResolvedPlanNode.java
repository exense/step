package step.core.artefacts.reports.resolvedplan;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.artefacts.AbstractArtefact;

public class ResolvedPlanNode extends AbstractIdentifiableObject {

    public final AbstractArtefact artefact;
    public final String artefactHash;
    public final String parentId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ResolvedPlanNode(@JsonProperty("artefact") AbstractArtefact artefact, @JsonProperty("artefactHash") String artefactHash,
                            @JsonProperty("parentId") String parentId) {
        this.artefact = artefact;
        this.artefactHash = artefactHash;
        this.parentId = parentId;
    }
}
