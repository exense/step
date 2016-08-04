package step.artefacts;

import step.artefacts.handlers.SwitchHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;

@Artefact(handler = SwitchHandler.class)
public class Switch extends AbstractArtefact {
	
	private String expression;

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}
}
