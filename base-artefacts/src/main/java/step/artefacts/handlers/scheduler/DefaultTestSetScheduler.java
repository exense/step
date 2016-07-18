package step.artefacts.handlers.scheduler;

import java.util.ArrayList;
import java.util.List;

import step.core.artefacts.AbstractArtefact;

public class DefaultTestSetScheduler extends TestSetScheduler {
	
	@Override
	public List<TestCaseBundle> bundleTestCases(List<AbstractArtefact> artefacts, int numberOfBundles) {
		
		List<TestCaseBundle> bundles = new ArrayList<TestCaseBundle>(numberOfBundles);
		for(int i=0;i<numberOfBundles;i++) {
			bundles.add(new TestCaseBundle());
		}
		
		int currentBundle = 0;
		for(AbstractArtefact artefact:artefacts) {
			bundles.get(currentBundle).getTestcases().add(artefact);
			currentBundle=(currentBundle+1)%numberOfBundles;					
		}
		
		return bundles;
	}

}
