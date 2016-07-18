package step.core.execution.model;

public class ReportExport {
	
	private String URL;
	
	private ReportExportStatus status;
	
	private String error;

	public ReportExport() {
		super();
	}

	public String getURL() {
		return URL;
	}

	public void setURL(String uRL) {
		URL = uRL;
	}

	public ReportExportStatus getStatus() {
		return status;
	}

	public void setStatus(ReportExportStatus status) {
		this.status = status;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

}
