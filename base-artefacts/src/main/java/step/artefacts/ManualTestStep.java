package step.artefacts;

import step.artefacts.handlers.ManualTestStepHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;

@Artefact(handler=ManualTestStepHandler.class)
public class ManualTestStep extends AbstractArtefact {
	
	boolean isComment;
	
	String description;

	public boolean isComment() {
		return isComment;
	}

	public void setComment(boolean isComment) {
		this.isComment = isComment;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
