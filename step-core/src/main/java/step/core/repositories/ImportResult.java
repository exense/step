package step.core.repositories;

import java.io.Serializable;
import java.util.List;

public class ImportResult implements Serializable {
	
	private static final long serialVersionUID = 3711110316457339962L;

	protected boolean successful = false;;
	
	protected String planId;
	
	List<String> errors;

	public boolean isSuccessful() {
		return successful;
	}

	public void setSuccessful(boolean successful) {
		this.successful = successful;
	}

	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
	}

	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}
}