package step.artefacts;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;

public class AbstractArtefactTest {

	public AbstractArtefactTest() {
		super();
	}

	protected ExecutionContext newExecutionContext() {
		return ExecutionEngine.builder().withPlugin(new BaseArtefactPlugin()).build().newExecutionContext();
	}

}