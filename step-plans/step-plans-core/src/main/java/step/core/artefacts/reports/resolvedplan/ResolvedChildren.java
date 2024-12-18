package step.core.artefacts.reports.resolvedplan;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ParentSource;

import java.util.List;

public class ResolvedChildren {

    public ParentSource parentSource;
    public List<AbstractArtefact> children;
    public String artefactPath;

    public ResolvedChildren(ParentSource parentSource, List<AbstractArtefact> children, String artefactPath) {
        this.parentSource = parentSource;
        this.children = children;
        this.artefactPath = artefactPath;
    }
}
