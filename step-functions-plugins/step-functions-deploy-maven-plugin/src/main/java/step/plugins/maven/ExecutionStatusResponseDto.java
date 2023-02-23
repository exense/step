package step.plugins.maven;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// ignore because the actual response contains much more fields than we need in maven-plugin
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutionStatusResponseDto {

	public static final String EXECUTION_FINAL_STATUS = "ENDED";
	public static final String REPORT_NODE_OK_STATUS = "PASSED";

	private String status;

	private String result;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}
}
