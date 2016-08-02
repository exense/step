package step.artefacts;

import step.artefacts.handlers.CheckHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.DynamicAttribute;

@Artefact(handler = CheckHandler.class, block=false)
public class Check extends AbstractArtefact {

	@DynamicAttribute
	private String expression;

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}
	
	
}
