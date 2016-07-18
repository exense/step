package step.common.managedoperations;

import java.util.Date;

public class Operation {

	private String name;
	
	private Date start;
	
	private Object details;

	public Operation(String name, Date start, Object details) {
		super();
		this.name = name;
		this.start = start;
		this.details = details;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Object getDetails() {
		return details;
	}

	public void setDetails(Object details) {
		this.details = details;
	}
}
