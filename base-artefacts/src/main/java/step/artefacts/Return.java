package step.artefacts;

import step.artefacts.handlers.ReturnHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.DynamicAttribute;

@Artefact(handler = ReturnHandler.class, block=false)
public class Return extends AbstractArtefact {

	@DynamicAttribute
	private String value;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
