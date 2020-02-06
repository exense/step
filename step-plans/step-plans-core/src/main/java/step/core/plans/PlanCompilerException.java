package step.core.plans;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class PlanCompilerException extends Exception {

	private final List<PlanCompilationError> errors = new ArrayList<>();

	public PlanCompilerException() {
		super();
	}

	public boolean addError(PlanCompilationError e) {
		return errors.add(e);
	}

	public List<PlanCompilationError> getErrors() {
		return errors;
	}
}
