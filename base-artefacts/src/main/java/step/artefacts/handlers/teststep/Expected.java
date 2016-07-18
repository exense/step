package step.artefacts.handlers.teststep;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import step.artefacts.Entry;

public class Expected {
	
	private String onFailure;
	
	private final List<Entry> setExpressions = new ArrayList<>();

	private final Map<String, String> checksExpressions = new TreeMap<>();

	public String getOnFailure() {
		return onFailure;
	}

	public void setOnFailure(String onFailure) {
		this.onFailure = onFailure;
	}

	public List<Entry> getSetExpressions() {
		return setExpressions;
	}

	public Map<String, String> getChecksExpressions() {
		return checksExpressions;
	}

}
