package step.plugins.functions.types;

import step.functions.Function;

public class CompositeFunction extends Function {

	String artefactId;
	
	public CompositeFunction() {
		super();
		executeLocally = true;
	}

	public String getArtefactId() {
		return artefactId;
	}

	public void setArtefactId(String artefactId) {
		this.artefactId = artefactId;
	}
}
