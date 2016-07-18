package step.artefacts;

import step.artefacts.handlers.TestSetHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;

@Artefact(name = "TestSet", handler = TestSetHandler.class)
public class TestSet extends AbstractArtefact {

	public TestSet() {
		super();
		setCreateSkeleton(true);
	}

}
