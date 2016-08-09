package step.artefacts.filters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
			Map<String, String> attributes = new HashMap<>();
			attributes = artefact.getAttributes();
			String name = attributes.get("name");
			return name!=null?includedNames.contains(name):false;
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
