package step.artefacts;

import step.artefacts.handlers.SetHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.DynamicAttribute;

@Artefact(handler = SetHandler.class, block=false)
public class Set extends AbstractArtefact {

	@DynamicAttribute
	private String key;
	
	private String expression;
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}
}
