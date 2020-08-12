package step.datapool.excel;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;

public class AbstractArtefactTest {

	public AbstractArtefactTest() {
		super();
	}

	protected ExecutionContext newExecutionContext() {
		return new ExecutionEngine().newExecutionContext();
	}

}