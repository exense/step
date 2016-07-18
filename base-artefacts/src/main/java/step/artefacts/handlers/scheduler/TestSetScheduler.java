package step.artefacts.handlers.scheduler;

import java.util.List;

import step.core.artefacts.AbstractArtefact;

public abstract class TestSetScheduler {

	public abstract List<TestCaseBundle> bundleTestCases(List<AbstractArtefact> artefacts, int numberOfBundles);
}
