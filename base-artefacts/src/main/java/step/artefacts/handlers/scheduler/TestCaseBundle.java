package step.artefacts.handlers.scheduler;

import java.util.ArrayList;
import java.util.List;

import step.core.artefacts.AbstractArtefact;

public class TestCaseBundle {
	
	private final List<AbstractArtefact> testcases = new ArrayList<AbstractArtefact>();

	private Throwable throwable;
	
	public List<AbstractArtefact> getTestcases() {
		return testcases;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}
}
