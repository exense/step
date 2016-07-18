package step.artefacts;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;

@Artefact(handler=CheckArtefactHandler.class)
public class CheckArtefact extends AbstractArtefact {

	private Runnable executionRunnable;

	public CheckArtefact() {
		super();
		// TODO Auto-generated constructor stub
	}

	public CheckArtefact(Runnable executionRunnable) {
		this(null, executionRunnable);
	}
	
	public CheckArtefact(String name, Runnable executionRunnable) {
		super();
		this.executionRunnable = executionRunnable;
		this.name = name;
	}

	public Runnable getExecutionRunnable() {
		return executionRunnable;
	}
	
}
