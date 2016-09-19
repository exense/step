package step.initialization;

import java.util.Date;

public class ControllerLog {

	private Date start;
	
	private String version;

	public ControllerLog() {
		super();
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
