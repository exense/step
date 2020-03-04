package step.common.managedoperations;

public class OperationDetails {
	public OperationDetails(String execId, String planId, String planName, String testcase,
			Operation operation) {
		super();
		this.execId = execId;
		this.planId = planId;
		this.planName = planName;
		this.testcase = testcase;
		this.operation = operation;
	}
	private String execId;
	private String planId;
	private String planName;
	private String testcase;
	private Operation operation;
	public String getExecId() {
		return execId;
	}
	public void setExecId(String execId) {
		this.execId = execId;
	}
	public String getPlanId() {
		return planId;
	}
	public void setPlanId(String planId) {
		this.planId = planId;
	}
	public String getPlanName() {
		return planName;
	}
	public void setPlanName(String planName) {
		this.planName = planName;
	}
	public Operation getOperation() {
		return operation;
	}
	public void setOperation(Operation operation) {
		this.operation = operation;
	}
	public String getTestcase() {
		return testcase;
	}
	public void setTestcase(String testcase) {
		this.testcase = testcase;
	}
}
