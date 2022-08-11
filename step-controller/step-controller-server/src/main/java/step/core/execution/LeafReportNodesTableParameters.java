package step.core.execution;

import step.framework.server.tables.service.TableParameters;

import java.util.List;

public class LeafReportNodesTableParameters extends TableParameters {
	private String eid;
	private List<String> testcases;

	public String getEid() {
		return eid;
	}

	public void setEid(String eid) {
		this.eid = eid;
	}

	public List<String> getTestcases() {
		return testcases;
	}

	public void setTestcases(List<String> testcases) {
		this.testcases = testcases;
	}
}
