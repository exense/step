package step.artefacts;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.threadpool.ThreadPoolPlugin;

public class AbstractArtefactTest {

	protected ExecutionEngine executionEngine;

	public AbstractArtefactTest() {
		super();
		executionEngine = ExecutionEngine.builder().withPlugin(new ThreadPoolPlugin()).withPlugin(new BaseArtefactPlugin()).build();
	}

	protected ExecutionContext newExecutionContext() {
		return executionEngine.newExecutionContext();
	}

}