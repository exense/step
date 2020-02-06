package step.core.plans;

import java.util.List;

public class PlanCompilationResult {
	
	private boolean hasError;
	
	private List<PlanCompilationError> errors;
	
	private Plan plan;

	public PlanCompilationResult() {
		super();
	}

	public boolean isHasError() {
		return hasError;
	}

	public void setHasError(boolean hasError) {
		this.hasError = hasError;
	}

	public List<PlanCompilationError> getErrors() {
		return errors;
	}

	public void setErrors(List<PlanCompilationError> errors) {
		this.errors = errors;
	}

	public Plan getPlan() {
		return plan;
	}

	public void setPlan(Plan plan) {
		this.plan = plan;
	}
}