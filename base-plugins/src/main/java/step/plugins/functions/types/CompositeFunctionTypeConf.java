package step.plugins.functions.types;

import step.functions.type.FunctionTypeConf;

public class CompositeFunctionTypeConf extends FunctionTypeConf {

	String artefactId;

	public CompositeFunctionTypeConf() {
		super();
	}

	public String getArtefactId() {
		return artefactId;
	}

	public void setArtefactId(String artefactId) {
		this.artefactId = artefactId;
	}
}
