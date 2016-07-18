package step.artefacts;

import step.artefacts.handlers.TestCaseHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;

@Artefact(handler = TestCaseHandler.class)
public class TestCase extends AbstractArtefact {

	public TestCase() {
		super();
		setCreateSkeleton(true);
	}
	
	
}
