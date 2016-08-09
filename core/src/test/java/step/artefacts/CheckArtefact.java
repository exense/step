package step.artefacts;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;

@Artefact(handler=CheckArtefactHandler.class)
public class CheckArtefact extends AbstractArtefact {

	private Runnable executionRunnable;

	public CheckArtefact() {
		super();
	}
	
	public CheckArtefact(Runnable executionRunnable) {
		super();
		this.executionRunnable = executionRunnable;
	}

	public Runnable getExecutionRunnable() {
		return executionRunnable;
	}
	
}
