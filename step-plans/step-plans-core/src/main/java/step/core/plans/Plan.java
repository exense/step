package step.core.plans;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnore;

import step.core.artefacts.AbstractArtefact;
import step.functions.Function;

public class Plan {

	protected AbstractArtefact root;
	
	protected Collection<AbstractArtefact> artefacts;
	
	protected Collection<Function> functions;
	
	public Plan(AbstractArtefact root, Collection<AbstractArtefact> artefacts) {
		super();
		this.root = root;
		this.artefacts = artefacts;
	}

	public Plan() {
		super();
	}

	@JsonIgnore
	public String getId() {
		return getRoot().getId().toString();
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
	
	public Collection<Function> getFunctions() {
		return functions;
	}

	public void setFunctions(Collection<Function> functions) {
		this.functions = functions;
	}
}
