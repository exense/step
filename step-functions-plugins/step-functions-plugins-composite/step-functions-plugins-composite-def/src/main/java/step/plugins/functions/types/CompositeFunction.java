package step.plugins.functions.types;

import step.functions.Function;

public class CompositeFunction extends Function {

	String artefactId;
	
	boolean executeLocally = true;

	public String getArtefactId() {
		return artefactId;
	}

	public void setArtefactId(String artefactId) {
		this.artefactId = artefactId;
	}

	public boolean isExecuteLocally() {
		return executeLocally;
	}

	public void setExecuteLocally(boolean executeLocally) {
		this.executeLocally = executeLocally;
	}

	@Override
	public boolean requiresLocalExecution() {
		return executeLocally;
	}
	
}
