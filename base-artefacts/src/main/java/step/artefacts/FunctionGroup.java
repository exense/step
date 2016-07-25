package step.artefacts;

import java.util.Map;

import step.artefacts.handlers.FunctionGroupHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;

@Artefact(handler = FunctionGroupHandler.class)
public class FunctionGroup extends AbstractArtefact {

	private Map<String, String> selectionCriteria;
	
	private Map<String, String> attributes;

	public Map<String, String> getSelectionCriteria() {
		return selectionCriteria;
	}

	public void setSelectionCriteria(Map<String, String> selectionCriteria) {
		this.selectionCriteria = selectionCriteria;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
}
