package step.artefacts;

import step.artefacts.handlers.IfBlockHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;

@Artefact(name = "If", handler = IfBlockHandler.class)
public class IfBlock extends AbstractArtefact {

	private String condition;

	public IfBlock() {
		super();
	}

	public IfBlock(String condition) {
		super();
		this.condition = condition;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}
}
