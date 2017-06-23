package step.planbuilder;

import java.util.Collection;

import step.core.artefacts.AbstractArtefact;

public class Plan {

	AbstractArtefact root;
	
	Collection<AbstractArtefact> artefacts;

	public Plan(AbstractArtefact root, Collection<AbstractArtefact> artefacts) {
		super();
		this.root = root;
		this.artefacts = artefacts;
	}

	public AbstractArtefact getRoot() {
		return root;
	}

	public void setRoot(AbstractArtefact root) {
		this.root = root;
	}

	public Collection<AbstractArtefact> getArtefacts() {
		return artefacts;
	}

	public void setArtefacts(Collection<AbstractArtefact> artefacts) {
		this.artefacts = artefacts;
	}
}
