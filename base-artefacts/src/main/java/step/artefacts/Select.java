package step.artefacts;

import step.artefacts.handlers.SelectHandler;
import step.core.artefacts.Artefact;
import step.core.artefacts.DynamicAttribute;
import step.core.artefacts.AbstractArtefact;

@Artefact(handler = SelectHandler.class)
public class Select extends AbstractArtefact {
	
	@DynamicAttribute
	private String var;

	public String getVar() {
		return var;
	}

	public void setVar(String var) {
		this.var = var;
	}

}
