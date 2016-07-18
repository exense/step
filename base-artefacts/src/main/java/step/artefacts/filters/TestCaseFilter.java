package step.artefacts.filters;

import java.util.ArrayList;
import java.util.List;

import step.artefacts.TestCase;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactFilter;

public class TestCaseFilter extends ArtefactFilter {
	
	private List<String> includedNames = new ArrayList<>();

	public TestCaseFilter() {
		super();
	}

	public TestCaseFilter(List<String> includedNames) {
		super();
		this.includedNames = includedNames;
	}

	@Override
	public boolean isSelected(AbstractArtefact artefact) {
		if(artefact instanceof TestCase) {
			return includedNames.contains(artefact.getName());
		} else {
			return true;
		}
	}

	public List<String> getIncludedNames() {
		return includedNames;
	}

	public void setIncludedNames(List<String> includedNames) {
		this.includedNames = includedNames;
	}

}
