package step.artefacts;

import step.artefacts.handlers.CaseHandler;
import step.core.artefacts.Artefact;
import step.core.artefacts.DynamicAttribute;
import step.core.artefacts.AbstractArtefact;

@Artefact(handler = CaseHandler.class)
public class Case extends AbstractArtefact {
	
	@DynamicAttribute
	private String value;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
