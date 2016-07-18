package step.core.repositories;

import java.util.ArrayList;
import java.util.List;

public class TestSetStatusOverview {

	String testsetName;
	
	List<TestRunStatus> runs = new ArrayList<>();

	public TestSetStatusOverview() {
		super();
	}
	
	public TestSetStatusOverview(String testsetName) {
		super();
		this.testsetName = testsetName;
	}

	public String getTestsetName() {
		return testsetName;
	}

	public void setTestsetName(String testsetName) {
		this.testsetName = testsetName;
	}

	public List<TestRunStatus> getRuns() {
		return runs;
	}

	public void setRuns(List<TestRunStatus> runs) {
		this.runs = runs;
	}
}
